package com.miqu.android.recitation.ui.reader

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.miqu.android.recitation.data.LexiconRepository
import com.miqu.android.recitation.data.RootsDatabaseHelper
import com.miqu.android.recitation.databinding.BottomSheetMorphologyBinding
import com.miqu.android.recitation.ui.lexicon.RootDetailActivity
import kotlin.concurrent.thread

class MorphologyBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_SURAH = "arg_surah"
        private const val ARG_VERSE = "arg_verse"

        fun newInstance(surah: Int, verse: Int): MorphologyBottomSheetFragment {
            return MorphologyBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SURAH, surah)
                    putInt(ARG_VERSE, verse)
                }
            }
        }
    }

    private var _binding: BottomSheetMorphologyBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MorphologyWordAdapter
    private lateinit var rootsDbHelper: RootsDatabaseHelper
    private lateinit var lexiconRepo: LexiconRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMorphologyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val surah = arguments?.getInt(ARG_SURAH) ?: 1
        val verse = arguments?.getInt(ARG_VERSE) ?: 1

        binding.textMorphologyTitle.text = "Word Morphology • Ayah $surah:$verse"
        binding.btnMorphologyClose.setOnClickListener {
            dismiss()
        }
        binding.textMorphologyAttribution.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("http://corpus.quran.com"))
                startActivity(intent)
            } catch (_: Exception) {}
        }

        rootsDbHelper = RootsDatabaseHelper.getInstance(requireContext())
        lexiconRepo = LexiconRepository(requireContext())

        adapter = MorphologyWordAdapter(
            onRootClick = { root ->
                val entry = lexiconRepo.getRootEntry(root)
                val intent = Intent(requireContext(), RootDetailActivity::class.java).apply {
                    putExtra(RootDetailActivity.EXTRA_ROOT, root)
                    putExtra(RootDetailActivity.EXTRA_DEFINITION, entry?.definition ?: "")
                }
                startActivity(intent)
            },
            onWordClick = { word ->
                val intent = Intent(requireContext(), WordDetailActivity::class.java).apply {
                    putExtra(WordDetailActivity.EXTRA_ARABIC_WORD, word.arabic)
                    putExtra(WordDetailActivity.EXTRA_CURRENT_MEANING, word.english)
                    putExtra(WordDetailActivity.EXTRA_ROOT, word.root)
                }
                startActivity(intent)
            }
        )

        binding.recyclerViewWords.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewWords.adapter = adapter

        thread(start = true, name = "verse-words-loader") {
            val words = rootsDbHelper.getWordsForVerse(surah, verse)
            val segmentsMap = rootsDbHelper.getCorpusSegmentsForVerse(surah, verse)
            val combined = words.map { word ->
                MorphologyWordAdapter.WordWithGrammar(
                    wordRoot = word,
                    segments = segmentsMap[word.position] ?: emptyList()
                )
            }
            activity?.runOnUiThread {
                adapter.submitList(combined)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
