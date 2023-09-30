package cs6235.a2.submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
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
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class PegNode
{

	public static enum SpecialNodeMarker
	{
		BEGIN, END, START, ENTRY, EXIT, WAIT, WAITING, NOTIFY, NOTIFYALL, NOTIFIEDENTRY, JOIN, BEFORECALL, AFTERCALL,
		HELLER
	}

	public static Set<PegNode> workList = new HashSet<>();
	public static Set<PegNode> allPegNodes = new HashSet<>();
	public static Set<PegNode> beginEndSet = new HashSet<>();
	public static HashMap<Node, Set<PegNode>> lockToNotifyNodesMap = new HashMap<>();
	public static HashMap<Node, Set<PegNode>> lockToWaitingNodesMap = new HashMap<>();
	public static HashMap<Node, Set<PegNode>> lockToMonitorMap = new HashMap<>();
	public static final HashMap<Unit, Set<PegNode>> startToBeginMap = new HashMap<>(); // start edge
	public static final HashMap<PegNode, Set<Unit>> beginToStartMap = new HashMap<>(); // reverse start edge
	public static final HashMap<PegNode, Set<PegNode>> notifyToNotifiedEntryMap = new HashMap<>(); // notify edge
	public static final HashMap<PegNode, Set<PegNode>> notifiedEntryToNotifyMap = new HashMap<>(); // reverse notify
																									// edge

	HashMap<EnterMonitorStmt, Node> syncScopeMap;
	Unit calledSootNode;
	Node residingThread;
	SpecialNodeMarker marker;
	Set<PegNode> localSucc = null;
	Set<PegNode> localPred = null;
	private final Set<PegNode> mPegNodes = new HashSet<>(); // m(n)
	public final Set<PegNode> mKill = new HashSet<>(); // kill(n)
	public final Set<PegNode> mGen = new HashSet<>(); // gen(n)
	public final Set<PegNode> mOut = new HashSet<>(); // out(n)

	@Override
	public String toString()
	{
		String printer = "SootNode : " + calledSootNode + "\n\t" + "syncScopeMap : " + syncScopeMap + "\n\t"
				+ "residing Thread : " + residingThread + "\n\t" + "marker :  " + marker;
		return printer;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((calledSootNode == null) ? 0 : calledSootNode.hashCode());
		result = prime * result + ((marker == null) ? 0 : marker.hashCode());
		result = prime * result + ((residingThread == null) ? 0 : residingThread.hashCode());
		result = prime * result + ((syncScopeMap == null) ? 0 : syncScopeMap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{

		if (this == obj)
		{
			return true;
		}

		if (!(obj instanceof PegNode))
		{
			return false;
		}
		PegNode other = (PegNode) obj;

		if (calledSootNode == null)
		{

			if (other.calledSootNode != null)
			{
				return false;
			}
		}
		else if (!calledSootNode.equals(other.calledSootNode))
		{
			return false;
		}

		if (marker != other.marker)
		{
			return false;
		}

		if (residingThread == null)
		{

			if (other.residingThread != null)
			{
				return false;
			}
		}
		else if (!residingThread.equals(other.residingThread))
		{
			return false;
		}

		if (syncScopeMap == null)
		{

			if (other.syncScopeMap != null)
			{
				return false;
			}
		}
		else if (!syncScopeMap.equals(other.syncScopeMap))
		{
			return false;
		}
		return true;
	}

	/**
	 * For every notify/notifyAll lock, map set of PegNodes
	 */
	public static void initializeLockToNotifyNodes()
	{

		for (PegNode peg : allPegNodes)
		{

			if (peg.marker == SpecialNodeMarker.NOTIFY || peg.marker == SpecialNodeMarker.NOTIFYALL)
			{

				for (EnterMonitorStmt ems : peg.syncScopeMap.keySet())
				{
					Node lock = peg.syncScopeMap.get(ems);

					if (lock != null && lock != KaamChalau.getLockFromPegNode(peg))
					{
						continue;
					}
					Set<PegNode> pegSet = lockToNotifyNodesMap.get(lock);

					if (pegSet == null)
					{
						pegSet = new HashSet<>();
						lockToNotifyNodesMap.put(lock, pegSet);
					}
					pegSet.add(peg);
				}
			}

		}
	}

	/**
	 * For every waiting lock, map set of PegNodes
	 */
	public static void initializeLockToWaitingNodes()
	{

		for (PegNode peg : allPegNodes)
		{

			if (peg.marker == SpecialNodeMarker.WAITING)
			{

				for (EnterMonitorStmt ems : peg.syncScopeMap.keySet())
				{
					Node lock = peg.syncScopeMap.get(ems);

					if (lock != null && lock != KaamChalau.getLockFromPegNode(peg))
					{
						continue;
					}
					Set<PegNode> pegSet = lockToWaitingNodesMap.get(lock);

					if (pegSet == null)
					{
						pegSet = new HashSet<>();
						lockToWaitingNodesMap.put(lock, pegSet);
					}
					pegSet.add(peg);
				}
			}

		}
	}

	public static void initializeLockToMonitor()
	{
		// Step 1 : traverse all the pegNodes
		// Step 2 : Add the present PegNode to the syncScopeMap value

		for (PegNode peg : allPegNodes)
		{

			for (Node lock : peg.syncScopeMap.values())
			{

				if (peg.marker == SpecialNodeMarker.WAITING || peg.marker == SpecialNodeMarker.NOTIFIEDENTRY)
				{

					if (lock == KaamChalau.getLockFromPegNode(peg))
					{
						continue;
					}

				}
				else if (peg.marker == SpecialNodeMarker.ENTRY)
				{
					EnterMonitorStmt pegEms = (EnterMonitorStmt) peg.calledSootNode;
					Node pegLock = peg.syncScopeMap.get(pegEms);

					if (lock == pegLock)
					{
						continue;
					}
				}
				Set<PegNode> pegSet = lockToMonitorMap.get(lock);

				if (pegSet == null)
				{
					pegSet = new HashSet<>();
					lockToMonitorMap.put(lock, pegSet);
				}
				pegSet.add(peg);
			}
		}

	}

	public static void initializeNotifyEdge()
	{

		for (PegNode peg : allPegNodes)
		{

			if (peg.marker == SpecialNodeMarker.NOTIFY || peg.marker == SpecialNodeMarker.NOTIFYALL)
			{
				notifyToNotifiedEntryMap.put(peg, new HashSet<>());
			}
			else if (peg.marker == SpecialNodeMarker.NOTIFIEDENTRY)
			{
				notifiedEntryToNotifyMap.put(peg, new HashSet<>());
			}
		}
	}

	public static void initializeWorklist()
	{

		for (PegNode peg : allPegNodes)
		{

			if (peg.marker == SpecialNodeMarker.START && peg.residingThread == null)
			{
				workList.add(peg);
			}
		}
	}

	public static void initializeGenSet()
	{

		for (PegNode peg : allPegNodes)
		{

			if (peg.marker == SpecialNodeMarker.START)
			{
				peg.mGen.addAll(startToBeginMap.get(peg.calledSootNode));
			}
		}
	}

	public PegNode getWaitingPred()
	{

		if (this.marker != SpecialNodeMarker.NOTIFIEDENTRY)
		{
			return null;
		}

		for (PegNode p : this.getLocalPegPreds())
		{
			return p;
		}

		return null;
	}

	public static void initializeKillSet()
	{

		for (PegNode peg : allPegNodes)
		{

			if (peg.marker == SpecialNodeMarker.JOIN)
			{

				for (Node t : KaamChalau.getSinglePointer(peg))
				{
					peg.mKill.addAll(getPegNodesOfThread(t));
				}
			}

			else if (peg.marker == SpecialNodeMarker.ENTRY)
			{
				peg.mKill.addAll(lockToMonitorMap.get(peg.syncScopeMap.get(((EnterMonitorStmt) peg.calledSootNode))));
			}

			else if (peg.marker == SpecialNodeMarker.NOTIFIEDENTRY)
			{
				Node node = KaamChalau.getLockFromPegNode(peg);

				peg.mKill.addAll(lockToMonitorMap.get(node));
			}

			else if (peg.marker == SpecialNodeMarker.NOTIFYALL)
			{
				Node node = KaamChalau.getLockFromPegNode(peg);

				if (lockToWaitingNodesMap.containsKey(node))
				{
					peg.mKill.addAll(lockToWaitingNodesMap.get(node));
				}
			}

			else if (peg.marker == SpecialNodeMarker.NOTIFY)
			{
				Node node = KaamChalau.getLockFromPegNode(peg);

				if (lockToWaitingNodesMap.containsKey(node) && lockToWaitingNodesMap.get(node).size() == 1)
				{
					peg.mKill.addAll(lockToWaitingNodesMap.get(node));

				}
			}

		}
	}

	public Set<PegNode> readMPegNodes()
	{
		Set<PegNode> mPegSet = new HashSet<>();
		mPegSet.addAll(this.mPegNodes);
		return mPegSet;
	}

	public boolean addToMPegNodes(PegNode peg)
	{
		boolean change = this.mPegNodes.add(peg);

		if (peg.mPegNodes.add(this))
		{
			PegNode.workList.add(peg);
		}
		return change;
	}

	public boolean addToMPegNodes(Set<PegNode> pegSet)
	{
		boolean checkChange = false;

		for (PegNode peg : pegSet)
		{
			checkChange = checkChange | addToMPegNodes(peg);
		}

		return checkChange;
	}

	/**
	 * For a given thread return all the PegNodes
	 * 
	 * @param thread
	 * @return
	 */
	public static Set<PegNode> getPegNodesOfThread(Node thread)
	{
		Set<PegNode> retThreadPegSet = new HashSet<>();

		for (PegNode peg : allPegNodes)
		{

			if (peg.residingThread == thread)
			{
				retThreadPegSet.add(peg);
			}
		}

		return retThreadPegSet;
	}

	public static Set<PegNode> constructBeginEnd(Node residingThread, HashMap<EnterMonitorStmt, Node> callingObj,
			Unit sootStmt)
	{
		// Step 1 :
		Set<PegNode> pegList = new HashSet<>();

		PegNode p = new PegNode(residingThread, null, callingObj, SpecialNodeMarker.BEGIN);
		PegNode po = new PegNode(residingThread, null, callingObj, SpecialNodeMarker.END);
		beginEndSet.add(p);
		beginEndSet.add(po);

		Set<PegNode> beginNodes = PegNode.startToBeginMap.get(sootStmt);

		if (beginNodes == null)
		{
			beginNodes = new HashSet<>();
			PegNode.startToBeginMap.put(sootStmt, beginNodes);
		}
		beginNodes.add(p);

		pegList.add(p);
		pegList.add(po);

		if (KaamChalau.printPegNode)
		{

			for (PegNode pg : pegList)
			{
				System.out.println("peg &: " + pg);
			}
		}

		return pegList;
	}

	public Set<PegNode> getLocalPegSuccs()
	{

		if (this.localSucc != null)
		{
			return this.localSucc;
		}

		this.localSucc = new HashSet<>();
		Stmt sootStmt = (Stmt) this.calledSootNode;
		UnitGraph cfg = MHPAnalysis.stmtToCFGMap.get(sootStmt);
		List<Unit> successOfStmt = null;

		if (cfg != null)
		{
			successOfStmt = cfg.getSuccsOf(sootStmt);
		}

		switch (this.marker)
		{
		case AFTERCALL:
			extractLocalPegSucc(sootStmt, successOfStmt);
			break;

		case BEFORECALL:
			List<Unit> list = new ArrayList<>();
			for (SootMethod sm : KaamChalau.getMethodsFromUnit(sootStmt))
			{
				UnitGraph calleeCfg = new BriefUnitGraph(sm.getActiveBody());
				list.add(calleeCfg.getHeads().get(0));
			}
			extractLocalPegSucc(null, list);

			break;

		case BEGIN:
			Node rt = this.residingThread;
			SootClass sc = ((RefType) rt.getType()).getSootClass();
			SootMethod sm = KaamChalau.getMethodByName(sc, "run");
//			SootMethod sm = sc.getMethodByName("run");
			Body b = sm.getActiveBody();
			UnitGraph g = new BriefUnitGraph(b);
			List<Unit> headList = new ArrayList<>();
			headList.add(g.getHeads().get(0));
			extractLocalPegSucc(null, headList);
			break;

		case END:
			// do-nothing
			break;

		case ENTRY:
		case JOIN:
		case NOTIFY:
		case NOTIFYALL:
		case START:
		case NOTIFIEDENTRY:
		case EXIT:
		case HELLER:
			extractLocalPegSucc(sootStmt, successOfStmt);
			break;

		case WAIT:
			Set<PegNode> pegListSet = MHPAnalysis.stmtToPegNodesMap.get(sootStmt);
			Set<PegNode> waitingPegList = new HashSet<>();

			for (PegNode p : pegListSet)
			{

				if (p.marker == SpecialNodeMarker.WAITING)
				{
					waitingPegList.add(p);
				}
			}
			filterLocalPegSucc(waitingPegList);
			break;

		case WAITING:
			pegListSet = MHPAnalysis.stmtToPegNodesMap.get(sootStmt);
			waitingPegList = new HashSet<>();

			for (PegNode p : pegListSet)
			{

				if (p.marker == SpecialNodeMarker.NOTIFIEDENTRY)
				{
					waitingPegList.add(p);
				}
			}
			filterLocalPegSucc(waitingPegList);

			break;

		default:
			assert (false);
		}
		return this.localSucc;
	}

	public Set<PegNode> getLocalPegPreds()
	{

		if (this.localPred == null)
		{
			PegNode.initializePreds();
		}
		return this.localPred;
	}

	private static void initializePreds()
	{

		for (PegNode peg : allPegNodes)
		{
			Set<PegNode> succSet = peg.getLocalPegSuccs();

			for (PegNode succPeg : succSet)
			{

				if (succPeg.localPred == null)
				{
					succPeg.localPred = new HashSet<>();
				}
				succPeg.localPred.add(peg);
			}
		}
	}

	/**
	 * Sets the localSucc set of a Peg node using the soot successors of its
	 * corresponding soot node.
	 * 
	 * @param successOfStmt
	 */
	private void extractLocalPegSucc(Unit sootStmt, List<Unit> successOfStmt)
	{

		// successOfStmt = null implies Begin or End NODe
		assert (successOfStmt != null);

		if (successOfStmt.isEmpty())
		{
			SootMethod sm = MHPAnalysis.stmtToCFGMap.get(sootStmt).getBody().getMethod();
			String funcName = sm.getName();

			if (funcName.equals("main"))
			{
				return;
			}
			else if (funcName.equals("run"))
			{
				Set<PegNode> endNodeSet = new HashSet<PegNode>();

				for (PegNode p : PegNode.beginEndSet)
				{

					if (p.marker == SpecialNodeMarker.END)
					{
						endNodeSet.add(p);
					}
				}
				filterLocalPegSucc(endNodeSet);
			}
			else
			{
				Unit callsite = KaamChalau.methodToCallsiteMap.get(sm);
				Set<PegNode> pegSet = MHPAnalysis.stmtToPegNodesMap.get(callsite);
				Set<PegNode> afterPegSet = new HashSet<>();

				for (PegNode peg : pegSet)
				{

					if (peg.marker == SpecialNodeMarker.AFTERCALL)
					{
						afterPegSet.add(peg);
					}
				}
				filterLocalPegSucc(afterPegSet);
			}
		}
		else
		{

			for (Unit u : successOfStmt)
			{
				Set<PegNode> pegSet = MHPAnalysis.stmtToPegNodesMap.get(u);
				VirtualInvokeExpr vie = KaamChalau.getFunctionCallExpr(u);

				if (vie != null)
				{
					String funcName = vie.getMethod().getName();
					Set<PegNode> retPegSet = new HashSet<PegNode>();

					if (KaamChalau.isASyncCall(u))
					{

						if (funcName.equals("wait"))
						{

							for (PegNode p : pegSet)
							{

								if (p.marker == SpecialNodeMarker.WAIT)
								{
									retPegSet.add(p);
								}
							}
							pegSet = retPegSet;
						}
					}
					else
					{

						for (PegNode p : pegSet)
						{

							if (p.marker == SpecialNodeMarker.BEFORECALL)
							{
								retPegSet.add(p);
							}
						}
						pegSet = retPegSet;
					}
				}

				if (sootStmt instanceof ExitMonitorStmt)
				{
					filterExitLocalPegSucc(pegSet);
				}
				else
				{
					filterLocalPegSucc(pegSet);
				}
			}
		}
	}

	/**
	 * Sets the localSucc set of a Peg node using the Peg nodes of soot successors
	 * of its corresponding soot node.
	 * 
	 * @param successOfStmt
	 */
	private void filterLocalPegSucc(Set<PegNode> pegSet)
	{
		outer: for (PegNode peg : pegSet)
		{

			if (peg.residingThread != this.residingThread)
			{
				continue;
			}

			if (!(peg.calledSootNode instanceof EnterMonitorStmt))
			{
				assert (peg.marker != SpecialNodeMarker.END || peg.syncScopeMap.isEmpty());

				if (!peg.syncScopeMap.equals(this.syncScopeMap))
				{
					continue;
				}
			}
			else
			{

				if (peg.syncScopeMap.keySet().size() != this.syncScopeMap.keySet().size() + 1)
				{
					continue;
				}

				for (EnterMonitorStmt ems : peg.syncScopeMap.keySet())
				{

					if (!this.syncScopeMap.containsKey(ems))
					{

						if (ems != peg.calledSootNode)
						{
							continue outer;
						}
					}
					else
					{

						if (!this.syncScopeMap.get(ems).equals(peg.syncScopeMap.get(ems)))
						{
							continue outer;
						}
					}

				}
			} // check syncScopeMap 0f Enter & Non-Enter Monitor Stmt

			this.localSucc.add(peg);

		} // pegNode_enumerate
	}

	/**
	 * Sets the localSucc set of a Peg node using the Peg nodes of soot successors
	 * of its corresponding soot node.
	 * 
	 * @param successOfStmt
	 */
	private void filterExitLocalPegSucc(Set<PegNode> pegSet)
	{
		outer: for (PegNode peg : pegSet)
		{

			if (peg.residingThread != this.residingThread)
			{
				continue;
			}

			if (!(peg.calledSootNode instanceof EnterMonitorStmt))
			{

				if (!peg.syncScopeMap.equals(this.syncScopeMap))
				{

					if (peg.syncScopeMap.size() + 1 != this.syncScopeMap.size())
					{
						continue;
					}
					else
					{

						for (EnterMonitorStmt ems : this.syncScopeMap.keySet())
						{

							if (peg.syncScopeMap.containsKey(ems))
							{

								if (!peg.syncScopeMap.get(ems).equals(this.syncScopeMap.get(ems)))
								{
									continue outer;
								}
							}
							else
							{
								assert (((ExitMonitorStmt) this.calledSootNode).getOp() == ems.getOp());

								if (((ExitMonitorStmt) this.calledSootNode).getOp() != ems.getOp())
								{
									continue outer;
								}

							}

						}
					}
				}
				else
				{
					// non enter-monitor
					continue;
				}
			}
			else
			{

				if (peg.syncScopeMap.keySet().size() != this.syncScopeMap.keySet().size())
				{
					continue;
				}
				int count = 0;

				for (EnterMonitorStmt ems : this.syncScopeMap.keySet())
				{

					if (!peg.syncScopeMap.containsKey(ems))
					{
						count++;
						continue;
					}
					else
					{

						if (!peg.syncScopeMap.get(ems).equals(this.syncScopeMap.get(ems)))
						{
							count++;
						}
					}
				}

				if (count > 1)
				{
					continue;
				}
			}

			this.localSucc.add(peg);

		} // pegNode_enumerate
	}

	public static void printPegSuccs()
	{

		for (PegNode peg : allPegNodes)
		{

			switch (peg.marker)
			{
			case AFTERCALL:
				System.err.println("Preds of AFTERCALL " + peg + " \n\t--->");
				for (PegNode succ : peg.getLocalPegPreds())
				{
					System.err.println("\t" + succ + "\n\t===");
				}
				break;

			case BEFORECALL:
				System.err.println("Succ of BEFORECALL " + peg + " \n\t--->");
				for (PegNode succ : peg.getLocalPegSuccs())
				{
					System.err.println("\t" + succ + "\n\t===");
				}
				break;

			case BEGIN:
			case END:
				break;

			case ENTRY:
				break;

			case EXIT:
				break;

			case HELLER:
				System.err.println("Succ of " + peg + " \n\t--->");
				for (PegNode succ : peg.getLocalPegSuccs())
				{
					System.err.println("\t" + succ + "\n\t===");
				}
				break;

			case JOIN:
				break;

			case NOTIFIEDENTRY:
				break;

			case NOTIFY:
				break;

			case NOTIFYALL:
				break;

			case START:
				break;

			case WAIT:
				break;

			case WAITING:
				break;

			default:
				break;

			}
		}
	}

	public static void initializeBeginToStartMap()
	{

		for (Unit u : startToBeginMap.keySet())
		{
			Set<PegNode> setPegger = startToBeginMap.get(u);

			for (PegNode peg : setPegger)
			{

				if (!beginToStartMap.containsKey(peg))
				{
					beginToStartMap.put(peg, new HashSet<Unit>());
				}

				Set<Unit> unitSet = beginToStartMap.get(peg);
				unitSet.add(u);
				beginToStartMap.put(peg, unitSet);
			}
		}
	}

	public static Set<PegNode> constructPegNodes(HashMap<EnterMonitorStmt, Node> callingObj, Unit calledSootNode,
			Node residingThread)
	{
		PegNode p = new PegNode(residingThread, calledSootNode, callingObj, SpecialNodeMarker.HELLER);

		Set<PegNode> listOfPegNodes = new HashSet<>();
		listOfPegNodes.add(p);

		if (calledSootNode instanceof AssignStmt)
		{
			AssignStmt a = (AssignStmt) calledSootNode;
			Value v = a.getRightOp();

			if (v instanceof InvokeExpr)
			{
				InvokeExpr ie = (InvokeExpr) v;
				// String funcName = ie.getMethod().getName();

				if (ie instanceof VirtualInvokeExpr)
				{

					// non-sync func call
					p.marker = SpecialNodeMarker.BEFORECALL;
					PegNode po = new PegNode(residingThread, calledSootNode, callingObj, SpecialNodeMarker.AFTERCALL);
					listOfPegNodes.add(po);
				}
				else // SpecialInvokeExpr
				{
				}
			}
			else
			{
				// not an InvokeExpr
			}
		}
		else if (calledSootNode instanceof InvokeStmt)
		{
			InvokeExpr ie = ((InvokeStmt) calledSootNode).getInvokeExpr();

			if (ie instanceof VirtualInvokeExpr)
			{
				String funcName = ie.getMethod().getName();

				if (funcName.equals("start"))
				{
					p.marker = SpecialNodeMarker.START;
				}
				else if (funcName.equals("wait"))
				{
					p.marker = SpecialNodeMarker.WAIT;

					PegNode waiting = new PegNode(residingThread, calledSootNode, callingObj,
							SpecialNodeMarker.WAITING);

					listOfPegNodes.add(waiting);

					PegNode notifiedEntry = new PegNode(residingThread, calledSootNode, callingObj,
							SpecialNodeMarker.NOTIFIEDENTRY);

					listOfPegNodes.add(notifiedEntry);

				}
				else if (funcName.equals("notify"))
				{
					p.marker = SpecialNodeMarker.NOTIFY;
				}
				else if (funcName.equals("notifyAll"))
				{
					p.marker = SpecialNodeMarker.NOTIFYALL;
				}
				else if (funcName.equals("join"))
				{
					p.marker = SpecialNodeMarker.JOIN;
				}
				else
				{
					// non-sync func call
					p.marker = SpecialNodeMarker.BEFORECALL;
					PegNode po = new PegNode(residingThread, calledSootNode, callingObj, SpecialNodeMarker.AFTERCALL);
					listOfPegNodes.add(po);
				}
			}
			else
			{ // do-nothing for constructors

			}
		}
		else if (calledSootNode instanceof EnterMonitorStmt)
		{
			p.marker = SpecialNodeMarker.ENTRY;
		}
		else if (calledSootNode instanceof ExitMonitorStmt)
		{
			p.marker = SpecialNodeMarker.EXIT;
		}
		else
		{
			// do-nothing for restcases
		}

		if (KaamChalau.printPegNode)
		{

			for (PegNode pg : listOfPegNodes)
			{
				System.out.println("peg : " + pg);
			}
		}
		return listOfPegNodes;
	}

	private PegNode(Node residingThread, Unit calledSootNode, HashMap<EnterMonitorStmt, Node> syncScopeMap,
			SpecialNodeMarker marker)
	{
		this.residingThread = residingThread;
		this.calledSootNode = calledSootNode;
		this.syncScopeMap = syncScopeMap;
		this.marker = marker;
		allPegNodes.add(this);
	}

}
