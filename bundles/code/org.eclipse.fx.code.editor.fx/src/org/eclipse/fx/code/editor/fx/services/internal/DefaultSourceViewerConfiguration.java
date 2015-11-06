package org.eclipse.fx.code.editor.fx.services.internal;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.fx.code.editor.Input;
import org.eclipse.fx.code.editor.fx.services.CompletionProposalPresenter;
import org.eclipse.fx.code.editor.fx.services.TextHoverMap;
import org.eclipse.fx.code.editor.services.CompletionProposal;
import org.eclipse.fx.code.editor.services.ProposalComputer;
import org.eclipse.fx.code.editor.services.ProposalComputer.ProposalContext;
import org.eclipse.fx.ui.controls.styledtext.TextSelection;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.AnnotationPresenter;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import javafx.scene.Node;

@SuppressWarnings("restriction")
public class DefaultSourceViewerConfiguration extends SourceViewerConfiguration {
	private final Input<?> input;
	private final PresentationReconciler reconciler;
	private final ProposalComputer proposalComputer;
	private final IAnnotationModel annotationModel;
	private final List<AnnotationPresenter> annotationPresenters;
	private final TextHoverMap hoverMap;
	private final CompletionProposalPresenter proposalPresenter;

	@Inject
	public DefaultSourceViewerConfiguration(
			Input<?> input,
			PresentationReconciler reconciler,
			@Optional ProposalComputer proposalComputer,
			@Optional IAnnotationModel annotationModel,
			@Optional AnnotationPresenter presenter,
			@Optional TextHoverMap hoverMap,
			@Optional CompletionProposalPresenter proposalPresenter
			) {
		this.input = input;
		this.hoverMap = hoverMap;
		this.reconciler = reconciler;
		this.proposalComputer = proposalComputer;
		this.annotationModel = annotationModel;
		this.proposalPresenter = proposalPresenter == null ? DefaultProposal::new : proposalPresenter;
		if( presenter != null ) {
			this.annotationPresenters = Collections.singletonList(presenter);
		} else {
			this.annotationPresenters = Collections.emptyList();
		}
	}

	@Override
	public String getStyleclassName() {
		return "source-viewer";
	}

	public final IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		return reconciler;
	}

	@Override
	public IContentAssistant getContentAssist() {
		if( proposalComputer != null ) {
			return new ContentAssistant(this::computeProposals);
		}
		return super.getContentAssist();
	}

	List<ICompletionProposal> computeProposals(Integer offset) {
		try {
			return proposalComputer.compute(new ProposalContext(input, offset)).get()
						.stream()
						.map(proposalPresenter::createProposal)
						.collect(Collectors.toList());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	@Override
	public IAnnotationModel getAnnotationModel() {
		return annotationModel;
	}

	@Override
	public List<AnnotationPresenter> getAnnotationPresenters() {
		return annotationPresenters;
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		if( hoverMap != null ) {
			return hoverMap.getHoverMap().get(contentType);
		}
		return super.getTextHover(sourceViewer, contentType);
	}

	static class DefaultProposal implements ICompletionProposal {
		private final CompletionProposal proposal;

		public DefaultProposal(CompletionProposal proposal) {
			this.proposal = proposal;
		}

		@Override
		public CharSequence getLabel() {
			return this.proposal.getLabel();
		}

		@Override
		public Node getGraphic() {
			return null;
		}

		@Override
		public void apply(IDocument document) {
			this.proposal.apply(document);
		}

		@Override
		public TextSelection getSelection(IDocument document) {
			org.eclipse.fx.code.editor.services.CompletionProposal.TextSelection selection = proposal.getSelection(document);
			return selection == null ? TextSelection.EMPTY : new TextSelection(selection.offset, selection.length);
		}
	}
}
