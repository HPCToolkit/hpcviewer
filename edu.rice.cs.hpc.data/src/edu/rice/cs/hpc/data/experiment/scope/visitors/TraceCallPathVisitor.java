package edu.rice.cs.hpc.data.experiment.scope.visitors;

import java.util.AbstractMap;
import java.util.HashMap;

import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.FileScope;
import edu.rice.cs.hpc.data.experiment.scope.GroupScope;
import edu.rice.cs.hpc.data.experiment.scope.LineScope;
import edu.rice.cs.hpc.data.experiment.scope.LoadModuleScope;
import edu.rice.cs.hpc.data.experiment.scope.LoopScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.ScopeVisitType;
import edu.rice.cs.hpc.data.experiment.scope.StatementRangeScope;


/*************************************************************
 * 
 * Class to iterate CCT and store the map from cpid to procedure scope
 *
 *************************************************************/
public class TraceCallPathVisitor implements IScopeVisitor 
{
	final private HashMap<Integer, Scope> mapCallpathToScope;
	
	public TraceCallPathVisitor() {
		mapCallpathToScope = new HashMap<Integer, Scope>();
	}
	
	public AbstractMap<Integer, Scope> getMap() {
		return mapCallpathToScope;
	}
	
	
	@Override
	public void visit(StatementRangeScope scope, ScopeVisitType vt) { }

	@Override
	public void visit(LoopScope scope, ScopeVisitType vt) { }

	@Override
	public void visit(CallSiteScope scope, ScopeVisitType vt) { }

	@Override
	public void visit(ProcedureScope scope, ScopeVisitType vt) { }

	@Override
	public void visit(FileScope scope, ScopeVisitType vt) { }

	@Override
	public void visit(GroupScope scope, ScopeVisitType vt) { }

	@Override
	public void visit(LoadModuleScope scope, ScopeVisitType vt) { }

	@Override
	public void visit(RootScope scope, ScopeVisitType vt) { }

	@Override
	public void visit(Scope scope, ScopeVisitType vt) { }

	@Override
	public void visit(LineScope scope, ScopeVisitType vt) { 
		if (vt == ScopeVisitType.PostVisit) {
			int cpid = scope.getCpid();
			if (cpid > 0) {
				Scope parent = scope.getParentScope();
				while (parent != null && 
						!isProcedureOrCallsite(parent)  ) {
					parent = parent.getParentScope();
				}
				if (parent != null && isProcedureOrCallsite(parent))
					mapCallpathToScope.put(Integer.valueOf(cpid), parent);
			}
		}
	}

	private boolean isProcedureOrCallsite(Scope scope) {
		return (scope instanceof ProcedureScope ||
				scope instanceof CallSiteScope  ||
				scope instanceof RootScope);
	}
}
