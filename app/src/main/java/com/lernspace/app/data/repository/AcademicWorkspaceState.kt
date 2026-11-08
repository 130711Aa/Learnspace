package com.lernspace.app.data.repository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import com.lernspace.app.ui.learning.SubjectUi
import com.lernspace.app.ui.learning.TopicStatus
import com.lernspace.app.ui.learning.TopicUi
import com.lernspace.app.ui.theme.AccentBlue
import com.lernspace.app.ui.theme.AccentTeal
import com.lernspace.app.ui.workspace.model.NoteUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Shared in-memory single source of truth for Academic Syllabus and Workspace Notes.
 * Connects Subjects, Syllabus Topics, and Interactive Note Canvas seamlessly.
 */
object AcademicWorkspaceState {

    val subjects = mutableStateListOf(
        SubjectUi(
            id = "subj-1",
            name = "Neurobiology",
            code = "NEUR-401",
            description = "Cellular physiology, ion channels, and synaptic plasticity",
            color = AccentTeal
        ),
        SubjectUi(
            id = "subj-2",
            name = "Cognitive Psychology",
            code = "PSYC-320",
            description = "Attention networks, working memory, and language processing",
            color = AccentBlue
        ),
        SubjectUi(
            id = "subj-3",
            name = "Advanced Statistics",
            code = "STAT-210",
            description = "Bayesian inference, probability distributions, and MCMC methods",
            color = Color(0xFF8B5CF6)
        ),
        SubjectUi(
            id = "subj-4",
            name = "Organic Chemistry II",
            code = "CHEM-222",
            description = "Reaction mechanisms, aromatic synthesis, and spectroscopy",
            color = Color(0xFFF59E0B)
        )
    )

    val topics = mutableStateListOf(
        // Neurobiology topics
        TopicUi(
            id = "top-1",
            subjectId = "subj-1",
            title = "Resting Membrane Potential & Nernst Equation",
            description = "Potassium leak channels and Gibbs-Donnan equilibrium",
            status = TopicStatus.COMFORTABLE,
            targetDate = "Nov 12"
        ),
        TopicUi(
            id = "top-2",
            subjectId = "subj-1",
            title = "Action Potential Propagation & Voltage Gating",
            description = "Hodgkin-Huxley model, absolute and relative refractory periods",
            status = TopicStatus.MASTERED,
            targetDate = "Nov 14"
        ),
        TopicUi(
            id = "top-3",
            subjectId = "subj-1",
            title = "Synaptic Transmission & LTP Plasticity",
            description = "NMDA receptor magnesium block release and CaMKII cascade",
            status = TopicStatus.NEEDS_REVIEW,
            targetDate = "Nov 16"
        ),
        TopicUi(
            id = "top-4",
            subjectId = "subj-1",
            title = "Hippocampal Circuitry & Spatial Mapping",
            description = "Trisynaptic circuit (DG -> CA3 -> CA1) and grid cell function",
            status = TopicStatus.LEARNING,
            targetDate = "Nov 20"
        ),
        TopicUi(
            id = "top-5",
            subjectId = "subj-1",
            title = "Glial Cell Regulation & Myelination",
            description = "Oligodendrocytes, astrocyte glutamate uptake, and tripartite synapse",
            status = TopicStatus.NOT_STARTED,
            targetDate = "Nov 25"
        ),

        // Cognitive Psychology topics
        TopicUi(
            id = "top-6",
            subjectId = "subj-2",
            title = "Baddeley's Multicomponent Working Memory",
            description = "Phonological loop, visuospatial sketchpad, and central executive",
            status = TopicStatus.COMFORTABLE,
            targetDate = "Nov 15"
        ),
        TopicUi(
            id = "top-7",
            subjectId = "subj-2",
            title = "Feature Integration Theory & Visual Search",
            description = "Treisman's preattentive feature map vs serial attentive binding",
            status = TopicStatus.LEARNING,
            targetDate = "Nov 18"
        ),
        TopicUi(
            id = "top-8",
            subjectId = "subj-2",
            title = "Dual Process Theory of Reasoning",
            description = "System 1 fast heuristic vs System 2 slow deliberate cognition",
            status = TopicStatus.NOT_STARTED,
            targetDate = "Nov 22"
        ),

        // Advanced Statistics topics
        TopicUi(
            id = "top-9",
            subjectId = "subj-3",
            title = "Bayesian Prior Distributions & Conjugacy",
            description = "Beta-Binomial, Normal-Normal conjugate priors for posterior estimation",
            status = TopicStatus.NEEDS_REVIEW,
            targetDate = "Nov 14"
        ),
        TopicUi(
            id = "top-10",
            subjectId = "subj-3",
            title = "Markov Chain Monte Carlo & Metropolis-Hastings",
            description = "Transition kernel, acceptance probability, and stationary distribution",
            status = TopicStatus.LEARNING,
            targetDate = "Nov 19"
        ),
        TopicUi(
            id = "top-11",
            subjectId = "subj-3",
            title = "Maximum Likelihood vs MAP Estimators",
            description = "Log-likelihood maximization, Newton-Raphson, and regularized priors",
            status = TopicStatus.COMFORTABLE,
            targetDate = "Nov 23"
        ),

        // Organic Chemistry topics
        TopicUi(
            id = "top-12",
            subjectId = "subj-4",
            title = "Electrophilic Aromatic Substitution (EAS)",
            description = "Arenium ion intermediate, activating vs deactivating ortho/para directors",
            status = TopicStatus.COMFORTABLE,
            targetDate = "Nov 13"
        ),
        TopicUi(
            id = "top-13",
            subjectId = "subj-4",
            title = "Aldol Condensation & Claisen Reactions",
            description = "Enolate ion nucleophilic attack on carbonyl carbons",
            status = TopicStatus.NEEDS_REVIEW,
            targetDate = "Nov 17"
        ),
        TopicUi(
            id = "top-14",
            subjectId = "subj-4",
            title = "1H & 13C NMR Spectroscopy Interpretation",
            description = "Chemical shift, spin-spin splitting multiplicity, and integration",
            status = TopicStatus.LEARNING,
            targetDate = "Nov 21"
        )
    )

    val notes = mutableStateListOf(
        NoteUiModel(
            id = "note-1",
            title = "LTP Induction & NMDA Receptors",
            subject = "Neurobiology",
            date = "Today, 10:15 AM",
            snippet = "Long-term potentiation requires high-frequency stimulation of Schaffer collaterals to relieve magnesium block...",
            subjectId = "subj-1",
            topicId = "top-3",
            topicName = "Synaptic Transmission & LTP Plasticity"
        ),
        NoteUiModel(
            id = "note-2",
            title = "Working Memory & Executive Control",
            subject = "Cognitive Psychology",
            date = "Oct 19",
            snippet = "Baddeley's multicomponent model includes the phonological loop, visuospatial sketchpad, and central executive...",
            subjectId = "subj-2",
            topicId = "top-6",
            topicName = "Baddeley's Multicomponent Working Memory"
        ),
        NoteUiModel(
            id = "note-3",
            title = "Bayesian Prior Distributions Summary",
            subject = "Advanced Statistics",
            date = "Yesterday",
            snippet = "Conjugate priors simplify posterior density calculation when sampling from exponential family distributions...",
            subjectId = "subj-3",
            topicId = "top-9",
            topicName = "Bayesian Prior Distributions & Conjugacy"
        ),
        NoteUiModel(
            id = "note-4",
            title = "Thesis Brainstorming & Literature Gap",
            subject = "Quick Scratchpad",
            date = "Just now",
            snippet = "Compare EEG power spectral density against fMRI BOLD signals in working memory load tasks...",
            isScratchpad = true
        )
    )

    // Helper queries
    fun getNotesForTopic(topicId: String): List<NoteUiModel> {
        return notes.filter { it.topicId == topicId }
    }

    fun getNotesForSubject(subjectId: String): List<NoteUiModel> {
        return notes.filter { it.subjectId == subjectId }
    }

    fun getScratchpads(): List<NoteUiModel> {
        return notes.filter { it.isScratchpad }
    }

    fun createNoteForTopic(subject: SubjectUi, topic: TopicUi): NoteUiModel {
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        val newNote = NoteUiModel(
            id = UUID.randomUUID().toString(),
            title = topic.title,
            subject = subject.name,
            date = "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
            snippet = "Start handwriting or typing lecture notes for ${topic.title}...",
            subjectId = subject.id,
            topicId = topic.id,
            topicName = topic.title,
            isScratchpad = false
        )
        notes.add(0, newNote)
        return newNote
    }

    fun createScratchpad(title: String = "Untitled Scratchpad"): NoteUiModel {
        val newNote = NoteUiModel(
            id = UUID.randomUUID().toString(),
            title = title,
            subject = "Quick Scratchpad",
            date = "Just now",
            snippet = "Freeform scratchpad for quick formulas, thoughts, or whiteboard sketch...",
            isScratchpad = true
        )
        notes.add(0, newNote)
        return newNote
    }
}

