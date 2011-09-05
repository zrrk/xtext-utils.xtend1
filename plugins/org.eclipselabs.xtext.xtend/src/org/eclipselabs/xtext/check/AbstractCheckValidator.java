/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipselabs.xtext.check;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.mwe.core.issues.IssuesImpl;
import org.eclipse.emf.mwe.core.issues.MWEDiagnostic;
import org.eclipse.xtend.check.CheckFacade;
import org.eclipse.xtend.expression.ExecutionContext;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.AbstractInjectableValidator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.FeatureBasedDiagnostic;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import com.google.inject.Inject;

/**
 * @author Sven Efftinge - Initial contribution and API
 * @author Michael Clay
 */
public abstract class AbstractCheckValidator extends AbstractInjectableValidator {

	private ExecutionContext ctx = null;

	@Inject
	public void setExecutionContext(ExecutionContext ctx) {
		this.ctx = ctx;
	}

	private final EnumMap<CheckType, Set<String>> checkFiles = new EnumMap<CheckType, Set<String>>(CheckType.class);

	public void addCheckFile(String checkFile, CheckType checkType) {
		Set<String> checks = checkFiles.get(checkType);
		if(checks == null) {
			checks = new LinkedHashSet<String>();
			checkFiles.put(checkType, checks);
		}
		checks.add(checkFile);
	}

	public void addCheckFile(String checkFile) {
		addCheckFile(checkFile, CheckType.FAST);
	}

	public boolean internalValidate(EClass eClass, EObject eObject, final DiagnosticChain diagnostics, Map<Object, Object> context) {
		CheckMode mode = CheckMode.getCheckMode(context);
		IssuesAdapter issues = new IssuesAdapter(diagnostics);
		for (final CheckType type: checkFiles.keySet()) {
			if (mode.shouldCheck(type)) {
				Set<String> list = checkFiles.get(type);
				for (String string : list) {
					issues.checkType = type;
					CheckFacade.checkAll(string, Collections.singleton(eObject), ctx, issues);
				}
			}
		}
		return !issues.hasErrors();
	}

	private static class IssuesAdapter extends IssuesImpl {
		DiagnosticChain diagnostics;
		CheckType checkType;

		private IssuesAdapter(DiagnosticChain diagnostics) {
			super();
			this.diagnostics = diagnostics;
		}

		@Override
		public void add(final MWEDiagnostic mweDiagnostic) {
			Object element = mweDiagnostic.getElement();
			EObject eObject = (element instanceof EObject) ? (EObject) element : null;
			Object property = mweDiagnostic.getData().get(1);
			EStructuralFeature eStructuralFeature = null;
			if (property instanceof String && eObject!=null) {
				eStructuralFeature = eObject.eClass().getEStructuralFeature(((String) property));
			}
			int severity = mweDiagnostic.getSeverity() == Diagnostic.WARNING ? Diagnostic.WARNING
					: Diagnostic.ERROR;
			
			diagnostics.add(createDiagnostic(severity, mweDiagnostic.getMessage(), eObject, eStructuralFeature, ValidationMessageAcceptor.INSIGNIFICANT_INDEX, checkType, null, ((String[])null)));
			super.add(mweDiagnostic);
		}
		protected Diagnostic createDiagnostic(int diagnosticSeverity, String message, EObject object, EStructuralFeature feature, int index, CheckType checkType, String code, String... issueData) {
			Diagnostic result = new FeatureBasedDiagnostic(diagnosticSeverity, message, object, feature, index, checkType, code, issueData);
			return result;
		}
		protected int toDiagnosticSeverity(Severity severity) {
			int diagnosticSeverity = -1;
			switch(severity) {
				case ERROR: diagnosticSeverity = Diagnostic.ERROR; break;
				case WARNING: diagnosticSeverity = Diagnostic.WARNING; break;
				case INFO: diagnosticSeverity = Diagnostic.INFO; break;
				default: throw new IllegalArgumentException("Unknow severity " + severity);
			}
			return diagnosticSeverity;
		}
	}
}
