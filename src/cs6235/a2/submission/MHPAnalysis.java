package cs6235.a2.submission;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs6235.a2.AnalysisBase;
import cs6235.a2.Query;
import cs6235.a2.submission.PegNode.SpecialNodeMarker;
import soot.Body;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.DoublePointsToSet;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class MHPAnalysis extends AnalysisBase
{

	public static HashMap<Unit, Set<PegNode>> stmtToPegNodesMap = new HashMap<>();
	public static final HashMap<Unit, UnitGraph> stmtToCFGMap = new HashMap<>();

	public static PointsToAnalysis pta;

	@Override
	public String getResultString()
	{
		String retString = "";
		outer: for (Query query : this.getQueries())
		{
			String lhsName = query.getLhs();
			String rhsName = query.getRhs();
			int indexOfColonLHS = lhsName.indexOf(":");
			int indexOfColonRHS = rhsName.indexOf(":");
			String classLHS = lhsName.substring(0, indexOfColonLHS);
			String classRHS = rhsName.substring(0, indexOfColonRHS);
			String methodLHS = lhsName.substring(indexOfColonLHS + 1);
			String methodRHS = rhsName.substring(indexOfColonRHS + 1);
			SootClass leftClass = Scene.v().getSootClass(classLHS);
			SootClass rightClass = Scene.v().getSootClass(classRHS);
			SootMethod leftMethod = KaamChalau.getMethodByName(leftClass, methodLHS);
//			SootMethod leftMethod = leftClass.getMethodByName(methodLHS);
			SootMethod rightMethod = KaamChalau.getMethodByName(rightClass, methodRHS);
			Unit leftUnit = KaamChalau.methodToCallsiteMap.get(leftMethod);
			Unit rightUnit = KaamChalau.methodToCallsiteMap.get(rightMethod);

			for (PegNode leftPeg : stmtToPegNodesMap.get(leftUnit))
			{

				for (PegNode rightPeg : stmtToPegNodesMap.get(rightUnit))
				{

					if (leftPeg.readMPegNodes().contains(rightPeg) || rightPeg.readMPegNodes().contains(leftPeg))
					{
						retString += "YES\n";
						continue outer;
					}
				}
			}
			retString += "NO\n";
		}

		return retString;
	}

	public static void processSyncBlock(Unit sootStmt, Node residingThread,
			HashMap<EnterMonitorStmt, Node> syncScopeMap, UnitGraph cfg)
	{
		// Step 0: Check whether SootStmt has already been processed, using KaamChalau

		EnterMonitorStmt ems = (EnterMonitorStmt) sootStmt;
		Value v = ems.getOp();
		DoublePointsToSet dps = (DoublePointsToSet) pta.reachingObjects((Local) v);

		dps.forall(new P2SetVisitor()
		{
			@Override
			public void visit(Node n)
			{
				HashMap<EnterMonitorStmt, Node> syncScopeMapNew = new HashMap<>(syncScopeMap);
				syncScopeMapNew.put((EnterMonitorStmt) sootStmt, n);

				if (MHPAnalysis.checkExistingPegNode(syncScopeMapNew, sootStmt, residingThread, null))
				{
					return;
				}
				Set<PegNode> pegList = PegNode.constructPegNodes(syncScopeMapNew, sootStmt, residingThread);

				if (!stmtToPegNodesMap.containsKey(sootStmt))
				{
					stmtToCFGMap.put(sootStmt, cfg);
					stmtToPegNodesMap.put(sootStmt, pegList);
				}
				else
				{
					stmtToPegNodesMap.get(sootStmt).addAll(pegList);
				}

				Set<Unit> currenSet = new HashSet<>();
				currenSet.addAll(cfg.getSuccsOf(sootStmt));

				Set<Unit> visited = new HashSet<>();

				while (!currenSet.isEmpty())
				{

					Set<Unit> nextSet = new HashSet<>();

					for (Unit s : currenSet)
					{

						if (!visited.contains(s))
						{
							visited.add(s);
							processStatement(s, cfg, residingThread, syncScopeMapNew);

							if (s instanceof ExitMonitorStmt)
							{
								continue;
							}
							nextSet.addAll(KaamChalau.getMeaningfulSuccessor(s, cfg));
						}
					}
					currenSet = nextSet;
				}

			}
		});

	}

	public static void processRun(Unit sootStmt, Node residingThread, HashMap<EnterMonitorStmt, Node> syncScopeMap)
	{
		syncScopeMap = new HashMap<EnterMonitorStmt, Node>();
		// Step 0 : Check whether the begin/end been already created for given <residing
		// thread,syncScopeMap>

		if (MHPAnalysis.checkExistingPegNodeForBeginEnd(syncScopeMap, residingThread))
		{
			return;
		}
		// Step 1: Create begin & end node

		PegNode.constructBeginEnd(residingThread, syncScopeMap, sootStmt);

		// Step 2: Identify the run method

		Type t = residingThread.getType();
		RefType rt = (RefType) t;
		SootClass className = rt.getSootClass();
		SootMethod m = KaamChalau.getMethodByName(className, "run");
//		SootMethod m = className.getMethodByName("run");
		processTargetedMethod(residingThread, syncScopeMap, m);

	}

	public static void processMethod(Unit sootStmt, Node residingThread, HashMap<EnterMonitorStmt, Node> syncScopeMap)
	{

		InvokeExpr ie = null;

		if (sootStmt instanceof AssignStmt)
		{
			AssignStmt stmt = (AssignStmt) sootStmt;
			ie = (InvokeExpr) stmt.getRightOp();
		}
		else if (sootStmt instanceof InvokeStmt)
		{
			ie = ((InvokeStmt) sootStmt).getInvokeExpr();
		}

		assert (ie != null);
		String funcName = KaamChalau.getFuncNameFromStmt((Stmt) sootStmt);

		Value base = ((VirtualInvokeExpr) ie).getBase();
		Local caller = (Local) base;
		PointsToSet pts = pta.reachingObjects(caller);

		Set<SootMethod> targetedMethods = new HashSet<>();
		((DoublePointsToSet) pts).forall(new P2SetVisitor()
		{

			@Override
			public void visit(Node n)
			{
				Type t = n.getType();
				RefType rt = (RefType) t;
				SootClass className = rt.getSootClass();
				SootMethod m = KaamChalau.getMethodByName(className, funcName);
//				SootMethod m = className.getMethodByName(funcName);
				targetedMethods.add(m);
			}
		});

		for (SootMethod sm : targetedMethods)
		{
//			System.err.println("Processing " + sm.getDeclaringClass().getName() + "::" + sm.getName());
			processTargetedMethod(residingThread, syncScopeMap, sm);
		}

	}

	public static void processTargetedMethod(Node residingThread, HashMap<EnterMonitorStmt, Node> syncScopeMap,
			SootMethod m)
	{
		KaamChalau.initializeMap(m);

		Body b = m.getActiveBody();
		UnitGraph g = new BriefUnitGraph(b);
		List<Unit> heads = g.getHeads();
		Stmt firstNode = (Stmt) heads.get(0);
		Set<Unit> currenSet = new HashSet<>();
		currenSet.add(firstNode);

		// Step 2 : Loop until currentSet is not Empty

		Set<Unit> visited = new HashSet<>();

		while (!currenSet.isEmpty())
		{
			// Step 2a : Loop over currentSet of succ

			Set<Unit> nextSet = new HashSet<>();

			for (Unit s : currenSet)
			{

				// Step 2a2 : For each stmt, process it
				if (!visited.contains(s))
				{
					visited.add(s);
					processStatement(s, g, residingThread, syncScopeMap);
					// Step 2a1 : Add more succ to the nextSet
					nextSet.addAll(KaamChalau.getMeaningfulSuccessor(s, g));
				}
			}

			// Step 2b : currentSet = nextSet
			currenSet = nextSet;
		}
	}

	/**
	 * Goal 1: Constructing pegNodes for the given statement in
	 * stmtToPegNodesMap(checking its existence with checkExistingPegNode) Goal 2:
	 * Maintaining CFG for the given statement in stmtToCFGMap
	 * 
	 * @param sootStmt
	 * @param methodCfg
	 * @param residingThread
	 * @param syncScopeMap
	 */
	public static void processStatement(Unit sootStmt, UnitGraph methodCfg, Node residingThread,
			HashMap<EnterMonitorStmt, Node> syncScopeMap)
	{

		if (sootStmt instanceof AssignStmt)
		{
			AssignStmt as = (AssignStmt) sootStmt;
			Value v = as.getRightOp();

			if (v instanceof VirtualInvokeExpr)
			{

				if (MHPAnalysis.checkExistingPegNode(syncScopeMap, sootStmt, residingThread, null))
				{
					return;
				}
				Set<PegNode> pegList = PegNode.constructPegNodes(syncScopeMap, sootStmt, residingThread);

				if (!stmtToPegNodesMap.containsKey(sootStmt))
				{
					stmtToCFGMap.put(sootStmt, methodCfg);
					stmtToPegNodesMap.put(sootStmt, pegList);
				}
				else
				{
					stmtToPegNodesMap.get(sootStmt).addAll(pegList);
				}

				processMethod(sootStmt, residingThread, syncScopeMap);

			}
			else
			{

				// rest of the cases. x=y or specialInvoke becauses we want the info to flow via
				// this node
				if (MHPAnalysis.checkExistingPegNode(syncScopeMap, sootStmt, residingThread, null))
				{
					return;
				}
				Set<PegNode> pegList = PegNode.constructPegNodes(syncScopeMap, sootStmt, residingThread);

				if (!stmtToPegNodesMap.containsKey(sootStmt))
				{
					stmtToCFGMap.put(sootStmt, methodCfg);
					stmtToPegNodesMap.put(sootStmt, pegList);
				}
				else
				{
					stmtToPegNodesMap.get(sootStmt).addAll(pegList);
				}

			}
		}
		else if (sootStmt instanceof InvokeStmt)
		{

			InvokeStmt is = (InvokeStmt) sootStmt;
			InvokeExpr ie = is.getInvokeExpr();

			if (ie instanceof VirtualInvokeExpr)
			{
				// Syn calls
				String funcName = ie.getMethod().getName();

				if (funcName.equals("start"))
				{

					if (MHPAnalysis.checkExistingPegNode(syncScopeMap, sootStmt, residingThread, null))
					{
						return;
					}
					Set<PegNode> pegList = PegNode.constructPegNodes(syncScopeMap, sootStmt, residingThread);

					if (!stmtToPegNodesMap.containsKey(sootStmt))
					{
						stmtToCFGMap.put(sootStmt, methodCfg);
						stmtToPegNodesMap.put(sootStmt, pegList);
					}
					else
					{
						stmtToPegNodesMap.get(sootStmt).addAll(pegList);
					}

					Value base = ((VirtualInvokeExpr) ie).getBase();
					Local caller = (Local) base;
					PointsToSet pts = pta.reachingObjects(caller);

					((DoublePointsToSet) pts).forall(new P2SetVisitor()
					{

						@Override
						public void visit(Node n)
						{
							processRun(sootStmt, n, syncScopeMap);
						}
					});

				}
				else if (funcName.equals("wait") || funcName.equals("notify") || funcName.equals("notifyAll")
						|| funcName.equals("join"))
				{

					if (MHPAnalysis.checkExistingPegNode(syncScopeMap, sootStmt, residingThread, null))
					{
						return;
					}
					Set<PegNode> pegList = PegNode.constructPegNodes(syncScopeMap, sootStmt, residingThread);

					if (!stmtToPegNodesMap.containsKey(sootStmt))
					{
						stmtToCFGMap.put(sootStmt, methodCfg);
						stmtToPegNodesMap.put(sootStmt, pegList);
					}
					else
					{
						stmtToPegNodesMap.get(sootStmt).addAll(pegList);
					}

				}
				else
				{

					// Calls other than Sync calls like x.foo()
					if (MHPAnalysis.checkExistingPegNode(syncScopeMap, sootStmt, residingThread, null))
					{
						return;
					}
					Set<PegNode> pegList = PegNode.constructPegNodes(syncScopeMap, sootStmt, residingThread);

					if (!stmtToPegNodesMap.containsKey(sootStmt))
					{
						stmtToCFGMap.put(sootStmt, methodCfg);
						stmtToPegNodesMap.put(sootStmt, pegList);
					}
					else
					{
						stmtToPegNodesMap.get(sootStmt).addAll(pegList);
					}

					processMethod(sootStmt, residingThread, syncScopeMap);
				}

			}
			else
			{
				// specialInvokeExpr

				if (MHPAnalysis.checkExistingPegNode(syncScopeMap, sootStmt, residingThread, null))
				{
					return;
				}
				Set<PegNode> pegList = PegNode.constructPegNodes(syncScopeMap, sootStmt, residingThread);

				if (!stmtToPegNodesMap.containsKey(sootStmt))
				{
					stmtToCFGMap.put(sootStmt, methodCfg);
					stmtToPegNodesMap.put(sootStmt, pegList);
				}
				else
				{
					stmtToPegNodesMap.get(sootStmt).addAll(pegList);
				}

			}
		}
		else if (sootStmt instanceof EnterMonitorStmt)
		{

			processSyncBlock(sootStmt, residingThread, syncScopeMap, methodCfg);

		}
		else if (sootStmt instanceof ExitMonitorStmt)
		{

			if (MHPAnalysis.checkExistingPegNode(syncScopeMap, sootStmt, residingThread, null))
			{
				return;
			}
			Set<PegNode> pegList = PegNode.constructPegNodes(syncScopeMap, sootStmt, residingThread);

			if (!stmtToPegNodesMap.containsKey(sootStmt))
			{
				stmtToCFGMap.put(sootStmt, methodCfg);
				stmtToPegNodesMap.put(sootStmt, pegList);
			}
			else
			{
				stmtToPegNodesMap.get(sootStmt).addAll(pegList);
			}
		}
		else
		{

			if (MHPAnalysis.checkExistingPegNode(syncScopeMap, sootStmt, residingThread, null))
			{
				return;
			}
			Set<PegNode> pegList = PegNode.constructPegNodes(syncScopeMap, sootStmt, residingThread);

			if (!stmtToPegNodesMap.containsKey(sootStmt))
			{
				stmtToCFGMap.put(sootStmt, methodCfg);
				stmtToPegNodesMap.put(sootStmt, pegList);
			}
			else
			{
				stmtToPegNodesMap.get(sootStmt).addAll(pegList);
			}

		}
	}

	public static void processMain()
	{
		// Step 1 : Initializing currentSet which processes succ

		Set<Unit> currenSet = new HashSet<>();
		SootMethod m = Scene.v().getMainMethod();
		KaamChalau.initializeMap(m);
		Body b = m.getActiveBody();
		UnitGraph g = new BriefUnitGraph(b);
		List<Unit> heads = g.getHeads();
		Stmt firstNode = (Stmt) heads.get(0);
		currenSet.add(firstNode);

		// Step 2 : Loop until currentSet is not Empty

		Set<Unit> visited = new HashSet<>();

		HashMap<EnterMonitorStmt, Node> syncScopeMap = new HashMap<>();

		while (!currenSet.isEmpty())
		{
			// Step 2a : Loop over currentSet of succ

			Set<Unit> nextSet = new HashSet<>();

			for (Unit s : currenSet)
			{

				// Step 2a2 : For each stmt, process it
				if (!visited.contains(s))
				{
					visited.add(s);
					processStatement(s, g, null, syncScopeMap);
					// Step 2a1 : Add more succ to the nextSet
					nextSet.addAll(KaamChalau.getMeaningfulSuccessor(s, g));
				}
			}

			// Step 2b : currentSet = nextSet
			currenSet = nextSet;
		}

	}

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options)
	{

		SootMethod mainMethod = Scene.v().getMainMethod();
		Body b = mainMethod.getActiveBody();
		UnitGraph g = new BriefUnitGraph(b);
		MHPAnalysis.pta = Scene.v().getPointsToAnalysis();
		processMain(); // creating the PEG nodes
		PegNode.initializeBeginToStartMap();
		PegNode.initializeNotifyEdge();
		PegNode.initializeLockToNotifyNodes();
		PegNode.initializeLockToWaitingNodes();
		PegNode.initializeLockToMonitor();
		driver();

		//////////////////////////
//		PegNode.printPegSuccs();

//		for (SootMethod m : KaamChalau.methodToCallsiteMap.keySet()) {
//			System.out.println("For method m below, the callSite is: " + KaamChalau.methodToCallsiteMap.get(m));
//			System.out.println(m);
//			System.out.println("*******");
//		}

		/*************************************************************************/
//		//say we want to obtain all classes in the scene that extend Thread
//		SootClass threadClass = Scene.v().getSootClass("java.lang.Thread");
//		List<SootClass> classes = Scene.v().getActiveHierarchy().getSubclassesOf(threadClass);
//		System.out.println(classes + " extend Thread");
//		System.out.println();
//		
//
//		//observe that it returned a bunch of library classes as well - you may filter them out by library classes, like so
//		//create a copy, because getSubclassesOf returns an unmodifiable collection
//		List<SootClass> filteredClasses = new LinkedList<SootClass>(classes);
//		filteredClasses.removeIf(c -> c.isLibraryClass());
//
//
//		System.out.println(filteredClasses);
//		System.out.println();

		/*************************************************************************/

//		//say we want to know the runtime types of each local in Main.main
//		SootMethod mainMethod = Scene.v().getMainMethod();
//		
//		for(Local local : mainMethod.getActiveBody().getLocals()) {
//			PointsToSet pts = pta.reachingObjects(local);
//			//cg.spark returns an instance of DoublePointsToSet
//			DoublePointsToSet doublePTS = (DoublePointsToSet) pta.reachingObjects(local);
//			
//			Set<Type> types = pts.possibleTypes();
//			System.out.println(local + ": types are " + types);
//			
//			//if you want to obtain the Soot Class corresponding to each of the ref types
//			for(Type type : types) {
//				if(type instanceof RefType) {
//					RefType ref = (RefType) type;
//					SootClass sC = ref.getSootClass();
//				}
//				System.out.println(type);
//			}
//			System.out.println();
//		}
		/*************************************************************************/

//		//now lets try to determine if two locals, say t1 and t2 are aliases
//
//		Local t1 = null;
//		Local t2 = null;
//		for(Local local : mainMethod.getActiveBody().getLocals()) {
//			if(local.getName().equals("t1"))
//				t1 = local;
//			else if(local.getName().equals("t2"))
//				t2 = local;
//			else 
//				continue;
//		}
//		
//
//		PointsToSet pts_t1 = pta.reachingObjects(t1);
//		PointsToSet pts_t2 = pta.reachingObjects(t2);
//		
//		boolean isAlias = pts_t1.hasNonEmptyIntersection(pts_t2);
//		System.out.println(isAlias);

		/*************************************************************************/

//		//interprocedural
//		//for the attached test case, lets check if t1.b and t2.b are the same object
//		//(it indeed should be, since it is the object used for synchronization)
//		SootMethod bRun = Scene.v().getSootClass("B").getMethodByName("run");
//		SootMethod cRun = Scene.v().getSootClass("C").getMethodByName("run");
//		
//		Local t1b = null;
//		Local t2b = null;
//		
//		for(Local local : bRun.getActiveBody().getLocals()) {
//			if (local.getName().equals("temp$0")) {
//				t1b = local;
//				break;
//			}
//		}
//		
//		for(Local local : cRun.getActiveBody().getLocals()) {
//			if (local.getName().equals("temp$0")) {
//				t2b = local;
//				break;
//			}
//		}
//		
//		PointsToSet pts_t1b = pta.reachingObjects(t1b);
//		PointsToSet pts_t2b = pta.reachingObjects(t2b);
//		System.out.println(pts_t1b.hasNonEmptyIntersection(pts_t2b));

		/*************************************************************************/

	}

	/**
	 * Method returns true iff the PEG Node already exists for the given
	 * stmt,obj,thread,marker
	 * 
	 * @param callingObj
	 * @param sootStmt
	 * @param residingThread
	 * @param marker
	 * @return
	 */
	public static boolean checkExistingPegNode(HashMap<EnterMonitorStmt, Node> callingObj, Unit sootStmt,
			Node residingThread, SpecialNodeMarker marker)
	{

		if (!stmtToPegNodesMap.containsKey(sootStmt))
		{
			return false;
		}

		Set<PegNode> pegList = stmtToPegNodesMap.get(sootStmt);

		for (PegNode peg : pegList)
		{

			if (!callingObj.equals(peg.syncScopeMap))
			{
				continue;
			}

			if (residingThread != peg.residingThread)
			{
				continue;
			}

			if (marker != null && marker != peg.marker)
			{
				continue;
			}
			return true;
		}

		return false;

	}

	/**
	 * 
	 * @param syncScopeMap
	 * @param residingThread
	 * @return
	 */
	public static boolean checkExistingPegNodeForBeginEnd(HashMap<EnterMonitorStmt, Node> syncScopeMap,
			Node residingThread)
	{

		for (PegNode p : PegNode.beginEndSet)
		{

			if (p.residingThread == residingThread && p.syncScopeMap.equals(syncScopeMap))
			{
				return true;
			}
		}
		return false;
	}

	public static PegNode returnSetElement(Set<PegNode> setList)
	{

		for (PegNode peg : setList)
		{
			return peg;
		}
		return null;
	}

	public static void driver()
	{
		firstStage();

		PegNode.initializeWorklist();

		while (!PegNode.workList.isEmpty())
		{
			PegNode currentPeg = returnSetElement(PegNode.workList);
			PegNode.workList.remove(currentPeg);
			processPegNode(currentPeg);
		}
	}

	public static void processPegNode(PegNode peg)
	{

		peg.addToMPegNodes(getMPegNodes(peg));

		if (peg.marker == SpecialNodeMarker.NOTIFY || peg.marker == SpecialNodeMarker.NOTIFYALL)
		{
			boolean change = false;
			change = PegNode.notifyToNotifiedEntryMap.get(peg).addAll(getNotifySucc(peg));

			for (PegNode nePeg : getNotifySucc(peg))
			{
				PegNode.notifiedEntryToNotifyMap.get(nePeg).add(peg);
			}

			if (change)
			{
				PegNode.workList.addAll(PegNode.notifyToNotifiedEntryMap.get(peg));
			}

			peg.mGen.addAll(PegNode.notifyToNotifiedEntryMap.get(peg));

		}

		boolean changed = peg.mOut.addAll(getOutPegNodes(peg));

		if (changed)
		{
			PegNode.workList.addAll(peg.getLocalPegSuccs());

			if (peg.marker == SpecialNodeMarker.START)
			{
				PegNode.workList.addAll(PegNode.startToBeginMap.get(peg.calledSootNode));
			}

		}

	}

	public static Set<PegNode> getMPegNodes(PegNode n)
	{
		Set<PegNode> retMPegNodes = new HashSet<>();

		if (n.marker == SpecialNodeMarker.BEGIN)
		{

			for (PegNode p : KaamChalau.getStartPred(n))
			{
				retMPegNodes.addAll(p.mOut);
			}

			retMPegNodes.removeAll(PegNode.getPegNodesOfThread(n.residingThread));
		}
		else if (n.marker == SpecialNodeMarker.NOTIFIEDENTRY)
		{

			for (PegNode p : PegNode.notifiedEntryToNotifyMap.get(n))
			{
				retMPegNodes.addAll(p.mOut);
			}

			retMPegNodes.retainAll(n.getWaitingPred().mOut);
			retMPegNodes.addAll(getGenNotifyAll(n));
		}
		else
		{

			for (PegNode p : n.getLocalPegPreds())
			{
				retMPegNodes.addAll(p.mOut);
			}
		}

		return retMPegNodes;
	}

	public static Set<PegNode> getGenNotifyAll(PegNode n)
	{
		Set<PegNode> retNotifiedEntryBhais = new HashSet<PegNode>();

		if (n.marker != SpecialNodeMarker.NOTIFIEDENTRY)
		{
			return retNotifiedEntryBhais;
		}
		Node lockN = KaamChalau.getLockFromPegNode(n);

		for (PegNode m : PegNode.allPegNodes)
		{

			if (m.marker != SpecialNodeMarker.NOTIFIEDENTRY || lockN != KaamChalau.getLockFromPegNode(m))
			{
				continue;
			}

			if (!m.getWaitingPred().readMPegNodes().contains(n.getWaitingPred()))
			{
				continue;
			}

			boolean found = false;

			for (PegNode r : PegNode.allPegNodes)
			{

				if (r.marker != SpecialNodeMarker.NOTIFYALL || lockN != KaamChalau.getLockFromPegNode(r))
				{
					continue;
				}

				if (!m.getWaitingPred().readMPegNodes().contains(r) || !n.getWaitingPred().readMPegNodes().contains(r))
				{
					continue;
				}
				found = true;
			}

			if (found)
			{
				retNotifiedEntryBhais.add(m);
			}

		}

		return retNotifiedEntryBhais;
	}

	public static Set<PegNode> getOutPegNodes(PegNode n)
	{
		Set<PegNode> retOut = new HashSet<>();
		retOut.addAll(n.readMPegNodes());
		retOut.addAll(getGenPegNodes(n));
		retOut.removeAll(n.mKill);
		return retOut;
	}

	public static Set<PegNode> getGenPegNodes(PegNode n)
	{

		if (n.marker == SpecialNodeMarker.EXIT)
		{
			Set<PegNode> genSet = new HashSet<>(n.mGen);

			Value v = ((ExitMonitorStmt) n.calledSootNode).getOp();

			for (EnterMonitorStmt ems : n.syncScopeMap.keySet())
			{

				if (ems.getOp() == v)
				{

					for (PegNode pegger : stmtToPegNodesMap.get(ems))
					{

						if (pegger.residingThread != n.residingThread)
						{
							continue;
						}

						if (!pegger.syncScopeMap.equals(n.syncScopeMap))
						{
							continue;
						}

						genSet.addAll(pegger.mKill);
					}
				}
			}
			n.mGen.addAll(genSet);
		}
		return n.mGen;
	}

	public static Set<PegNode> getNotifySucc(PegNode n)
	{
		Set<PegNode> retNotifySucc = new HashSet<>();

		if (n.marker != SpecialNodeMarker.NOTIFY && n.marker != SpecialNodeMarker.NOTIFYALL)
		{
			return retNotifySucc;
		}

		Node lockN = KaamChalau.getLockFromPegNode(n);

		for (PegNode m : PegNode.allPegNodes)
		{

			if (m.marker != SpecialNodeMarker.NOTIFIEDENTRY || lockN != KaamChalau.getLockFromPegNode(m))
			{
				continue;
			}

			if (!n.readMPegNodes().contains(m.getWaitingPred()))
			{
				continue;
			}

			retNotifySucc.add(m);
		}

		return retNotifySucc;

	}

	public static void firstStage()
	{
		PegNode.initializeGenSet();
		PegNode.initializeKillSet();
	}

}
