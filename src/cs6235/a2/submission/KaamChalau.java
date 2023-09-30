package cs6235.a2.submission;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cs6235.a2.submission.PegNode.SpecialNodeMarker;
import soot.Local;
import soot.RefType;
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
import soot.toolkits.graph.UnitGraph;

public class KaamChalau
{

	public static final HashMap<SootMethod, Unit> methodToCallsiteMap = new HashMap<>();
	public static boolean printPegNode = false;

	public static Set<SootMethod> getMethodsFromUnit(Unit u)
	{
		VirtualInvokeExpr ie = null;

		if (u instanceof AssignStmt)
		{
			AssignStmt as = (AssignStmt) u;

			if (as.getRightOp() instanceof VirtualInvokeExpr)
			{
				ie = (VirtualInvokeExpr) as.getRightOp();
			}
		}
		else if (u instanceof InvokeStmt)
		{
			InvokeStmt is = (InvokeStmt) u;
			InvokeExpr i = is.getInvokeExpr();

			if (i instanceof VirtualInvokeExpr)
			{
				VirtualInvokeExpr vie = (VirtualInvokeExpr) i;
				String funcName = vie.getMethod().getName();

				if (!(funcName.equals("wait") || funcName.equals("notify") || funcName.equals("notifyAll")
						|| funcName.equals("start") || funcName.equals("join")))
				{
					ie = vie;
				}

			}

		}

		if (ie == null)
		{
			return null;
		}
		// obtain SootMethod from base of ie
		Value receiver = ie.getBase();
		Set<Type> types = MHPAnalysis.pta.reachingObjects((Local) receiver).possibleTypes();
		Set<SootMethod> sootMethodSet = new HashSet<SootMethod>();
		String funcName = ie.getMethod().getName();
		SootMethod ss = null;

		for (Type t : types)
		{
			RefType rt = (RefType) t;
			ss = KaamChalau.getMethodByName(rt.getSootClass(), funcName);
//			ss = rt.getSootClass().getMethodByName(funcName);

			if (ss != null)
			{
				sootMethodSet.add(ss);
			}
		}
//		assert (sootMethodSet.size() == 1) : "Found a callsite with multiple targets(polymorphic)" + u;

		return sootMethodSet;
	}

	/**
	 * Goal 1 : Maintaining all the callsite t.foo() for the given foo() in
	 * methodToCallsiteMap.
	 * 
	 * @param sm
	 */
	public static void initializeMap(SootMethod sm)
	{
		assert (sm.hasActiveBody());

		for (Unit u : sm.getActiveBody().getUnits())
		{
			VirtualInvokeExpr ie = null;

			if (u instanceof AssignStmt)
			{
				AssignStmt as = (AssignStmt) u;

				if (as.getRightOp() instanceof VirtualInvokeExpr)
				{
					ie = (VirtualInvokeExpr) as.getRightOp();
				}
			}
			else if (u instanceof InvokeStmt)
			{
				InvokeStmt is = (InvokeStmt) u;
				InvokeExpr i = is.getInvokeExpr();

				if (i instanceof VirtualInvokeExpr)
				{
					VirtualInvokeExpr vie = (VirtualInvokeExpr) i;
					String funcName = vie.getMethod().getName();

					if (!(funcName.equals("wait") || funcName.equals("notify") || funcName.equals("notifyAll")
							|| funcName.equals("start") || funcName.equals("join")))
					{
						ie = vie;
					}

				}

			}

			if (ie == null)
			{
				continue;
			}
			// obtain SootMethod from base of ie
			Value receiver = ie.getBase();
			Set<Type> types = MHPAnalysis.pta.reachingObjects((Local) receiver).possibleTypes();
			Set<SootMethod> sootMethodSet = new HashSet<SootMethod>();
			String funcName = ie.getMethod().getName();
			SootMethod ss = null;

			for (Type t : types)
			{
				RefType rt = (RefType) t;
				ss = KaamChalau.getMethodByName(rt.getSootClass(), funcName);
//				ss = rt.getSootClass().getMethodByName(funcName);

				if (ss != null)
				{
					sootMethodSet.add(ss);
				}

				if (ss != null)
				{
					KaamChalau.methodToCallsiteMap.put(ss, u);
				}
			}

//			if (sootMethodSet.size() > 1) {
//				System.err.println("Found a callsite with multiple targets(polymorphic)" + u);
//			}

		}
	}

	/**
	 * Checks whether the stmt is a function call (of either AssignStmt or
	 * InvokeStmt).
	 * 
	 * @param stmt
	 * @return
	 */
	public static VirtualInvokeExpr getFunctionCallExpr(Unit stmt)
	{

		if (stmt instanceof AssignStmt)
		{
			AssignStmt as = (AssignStmt) stmt;
			Value v = as.getRightOp();

			if (v instanceof VirtualInvokeExpr)
			{
				return (VirtualInvokeExpr) v;
			}
		}
		else if (stmt instanceof InvokeStmt)
		{
			InvokeStmt is = (InvokeStmt) stmt;
			InvokeExpr ie = is.getInvokeExpr();

			if (ie instanceof VirtualInvokeExpr)
			{
				return (VirtualInvokeExpr) ie;
			}
		}

		return null;
	}

	/**
	 * Checks whether stmt corresponds to a synchronization call.
	 * 
	 * @param stmt
	 * @return
	 */
	public static boolean isASyncCall(Unit stmt)
	{
		VirtualInvokeExpr vie = KaamChalau.getFunctionCallExpr(stmt);

		if (vie == null)
		{
			return false;
		}

		String funcName = vie.getMethod().getName();

		if (funcName.equals("wait") || funcName.equals("notifyAll") || funcName.equals("notify")
				|| funcName.equals("start") || funcName.equals("join"))
		{
			return true;
		}
		return false;
	}

	public static String getFuncNameFromStmt(Stmt stmt)
	{
		VirtualInvokeExpr vie = KaamChalau.getFunctionCallExpr(stmt);

		if (vie == null)
		{
			return null;
		}

		return vie.getMethod().getName();
	}

	/**
	 * For t.join it returns the receiver if it points to a single reference
	 * 
	 * @param peg
	 * @return
	 */
	public static Set<Node> getSinglePointer(PegNode peg)
	{
		assert (peg.marker == SpecialNodeMarker.JOIN);
		Unit stmt = peg.calledSootNode;
		VirtualInvokeExpr vie = getFunctionCallExpr(stmt);
		Local l = (Local) vie.getBase();
		DoublePointsToSet dps = (DoublePointsToSet) MHPAnalysis.pta.reachingObjects(l);
		Set<Node> setNode = new HashSet<>();

		dps.forall(new P2SetVisitor()
		{

			@Override
			public void visit(Node arg0)
			{
				setNode.add(arg0);
			}
		});

		return setNode;
	}

	public static List<Unit> getMeaningfulSuccessor(Unit sootStmt, UnitGraph callingMethodCfg)
	{

		if (sootStmt instanceof EnterMonitorStmt)
		{
			Set<Unit> setlist = new HashSet<>();
			setlist.add(sootStmt);
			EnterMonitorStmt enter = (EnterMonitorStmt) sootStmt;

			while (true)
			{

				for (Unit u : setlist)
				{

					if (u instanceof ExitMonitorStmt)
					{
						ExitMonitorStmt ems = (ExitMonitorStmt) u;

						if (enter.getOp().equals(ems.getOp()))
						{
							return callingMethodCfg.getSuccsOf(u);
						}
					}

				}
				Set<Unit> nextSetList = new HashSet<Unit>();

				for (Unit u : setlist)
				{
					nextSetList.addAll(callingMethodCfg.getSuccsOf(u));
				}

				if (nextSetList.isEmpty())
				{
					assert (false);
					return null;
				}
				setlist = nextSetList;
			}
		}
		else
		{
			return callingMethodCfg.getSuccsOf(sootStmt);
		}
	}

	public static Set<PegNode> getStartPred(PegNode n)
	{
		Set<PegNode> retStartPred = new HashSet<>();

		if (n.marker != SpecialNodeMarker.BEGIN)
		{
			return retStartPred;
		}

		for (Unit u : PegNode.beginToStartMap.get(n))
		{
			retStartPred.addAll(MHPAnalysis.stmtToPegNodesMap.get(u));
		}

		return retStartPred;
	}

	public static Node getLockFromPegNode(PegNode peg)
	{

		Unit stmt = (Unit) peg.calledSootNode;
		VirtualInvokeExpr vie = getFunctionCallExpr(stmt);
		Local l = (Local) vie.getBase();
		Node node = null;
		DoublePointsToSet dps = (DoublePointsToSet) MHPAnalysis.pta.reachingObjects(l);

		for (EnterMonitorStmt ems : peg.syncScopeMap.keySet())
		{

			if (dps.contains(peg.syncScopeMap.get(ems)))
			{
				node = peg.syncScopeMap.get(ems);
				break;
			}

		}
		return node;
	}

	public static boolean useThis = true;

	public static SootMethod getMethodByName(SootClass sc, String name)
	{

		if (!useThis)
		{
			return sc.getMethodByName(name);
		}

		do
		{
			SootMethod sm = sc.getMethodByName(name);

			if (sm != null)
			{
				return sm;
			}

			if (!sc.hasSuperclass())
			{
				break;
			}
			sc = sc.getSuperclass();
		} while (true);

		return null;

	}

}
