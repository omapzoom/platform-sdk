/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt;

import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.ddms.ISourceRevealer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * Implementation of the com.android.ide.ddms.sourceRevealer extension point.
 * Note that this code is duplicated in the PDT plugin's SourceRevealer as well.
 */
public class SourceRevealer implements ISourceRevealer {
    @Override
    public boolean reveal(String applicationName, String className, int line) {
        IProject project = ProjectHelper.findAndroidProjectByAppName(applicationName);
        if (project != null) {
            return BaseProjectHelper.revealSource(project, className, line);
        }

        return false;
    }

    @Override
    public boolean revealLine(String fileName, int lineNumber) {
        SearchEngine se = new SearchEngine();
        SearchPattern searchPattern = SearchPattern.createPattern(
                fileName,
                IJavaSearchConstants.CLASS,
                IJavaSearchConstants.DECLARATIONS,
                SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
        LineSearchRequestor requestor = new LineSearchRequestor(lineNumber);
        try {
            se.search(searchPattern,
                    new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
                    SearchEngine.createWorkspaceScope(),
                    requestor,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            AdtPlugin.printErrorToConsole(e.getMessage());
            return false;
        }

        return requestor.didMatch();
    }

    @Override
    public boolean revealMethod(String fqmn) {
        SearchEngine se = new SearchEngine();
        SearchPattern searchPattern = SearchPattern.createPattern(
                fqmn,
                IJavaSearchConstants.METHOD,
                IJavaSearchConstants.DECLARATIONS,
                SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
        MethodSearchRequestor requestor = new MethodSearchRequestor();
        try {
            se.search(searchPattern,
                    new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
                    SearchEngine.createWorkspaceScope(),
                    requestor,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            AdtPlugin.printErrorToConsole(e.getMessage());
            return false;
        }

        return requestor.didMatch();
    }

    private static class LineSearchRequestor extends SearchRequestor {
        private boolean mFoundMatch = false;
        private int mLineNumber;

        public LineSearchRequestor(int lineNumber) {
            mLineNumber = lineNumber;
        }

        public boolean didMatch() {
            return mFoundMatch;
        }

        @Override
        public void acceptSearchMatch(SearchMatch match) throws CoreException {
            if (match.getResource() instanceof IFile && !mFoundMatch) {
                IFile matchedFile = (IFile) match.getResource();
                IMarker marker = matchedFile.createMarker(IMarker.TEXT);
                marker.setAttribute(IMarker.LINE_NUMBER, mLineNumber);
                IDE.openEditor(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
                        marker);
                marker.delete();
                mFoundMatch = true;
            }
        }
    }

    private static class MethodSearchRequestor extends SearchRequestor {
        private boolean mFoundMatch = false;

        public boolean didMatch() {
            return mFoundMatch;
        }

        @Override
        public void acceptSearchMatch(SearchMatch match) throws CoreException {
            Object element = match.getElement();
            if (element instanceof IMethod && !mFoundMatch) {
                IMethod method = (IMethod) element;
                JavaUI.openInEditor(method);
                mFoundMatch = true;
            }
        }
    }
}
