package com.miqu.android.recitation.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.miqu.android.recitation.data.TafsirRepository
import com.miqu.android.recitation.data.UserSettings
import com.miqu.android.recitation.databinding.BottomSheetTafsirBinding
import io.noties.markwon.Markwon
import kotlin.concurrent.thread

class TafsirBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_SURAH = "arg_surah"
        private const val ARG_VERSE = "arg_verse"
        private const val ARG_GLOBAL_ID = "arg_global_id"

        fun newInstance(surah: Int, verse: Int, globalId: Int): TafsirBottomSheetFragment {
            return TafsirBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SURAH, surah)
                    putInt(ARG_VERSE, verse)
                    putInt(ARG_GLOBAL_ID, globalId)
                }
            }
        }
    }

    private var _binding: BottomSheetTafsirBinding? = null
    private val binding get() = _binding!!

    private lateinit var tafsirRepo: TafsirRepository
    private lateinit var userSettings: UserSettings
    private lateinit var markwon: Markwon
    private var selectedTafsirFile: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTafsirBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val surah = arguments?.getInt(ARG_SURAH) ?: 1
        val verse = arguments?.getInt(ARG_VERSE) ?: 1
        val globalId = arguments?.getInt(ARG_GLOBAL_ID) ?: 1

        binding.textTafsirTitle.text = "Tafsir • Ayah $surah:$verse"
        binding.btnTafsirClose.setOnClickListener {
            dismiss()
        }

        tafsirRepo = TafsirRepository(requireContext())
        userSettings = UserSettings(requireContext())
        markwon = Markwon.create(requireContext())
        selectedTafsirFile = userSettings.tafsir

        val tafsirBooks = TafsirRepository.AVAILABLE_TAFSIRS
        val displayList = tafsirBooks.map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayList)
        binding.dropdownTafsirSelector.setAdapter(adapter)

        val currentIndex = tafsirBooks.indexOfFirst { it.fileName == selectedTafsirFile }.coerceAtLeast(0)
        binding.dropdownTafsirSelector.setText(displayList[currentIndex], false)

        loadTafsirContent(globalId)

        binding.dropdownTafsirSelector.setOnItemClickListener { _, _, position, _ ->
            selectedTafsirFile = tafsirBooks[position].fileName
            loadTafsirContent(globalId)
        }
    }

    private fun loadTafsirContent(globalId: Int) {
        binding.textTafsirContent.text = "Loading exegesis..."
        thread(start = true, name = "tafsir-loader") {
            val content = tafsirRepo.getTafsirForVerse(selectedTafsirFile, globalId)
            activity?.runOnUiThread {
                markwon.setMarkdown(binding.textTafsirContent, content)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
