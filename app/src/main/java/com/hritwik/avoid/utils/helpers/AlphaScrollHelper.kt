package com.hritwik.avoid.utils.helpers

import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.state.LibraryGridItem
import java.text.Normalizer









fun normalizeChar(char: Char?): Char {
    if (char == null) return '#'

    
    if (char.isDigit()) return '#'

    
    if (char.isLetter()) {
        
        val normalized = Normalizer.normalize(char.toString(), Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "") 
            .uppercase()
            .firstOrNull()

        
        return if (normalized != null && normalized in 'A'..'Z') {
            normalized
        } else {
            '#'
        }
    }

    
    return '#'
}




















fun buildSectionedGridItems(items: List<MediaItem>): Pair<List<LibraryGridItem>, Map<Char, Int>> {
    if (items.isEmpty()) {
        return Pair(emptyList(), emptyMap())
    }

    
    
    
    val sortedItems = items.sortedBy { item ->
        val name = item.name.trim()
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
            .lowercase()
    }

    val sectionedItems = mutableListOf<LibraryGridItem>()
    val headerIndices = mutableMapOf<Char, Int>()

    var currentLetter: Char? = null
    var headerCount = 0  

    sortedItems.forEach { item ->
        
        val firstChar = item.name.trim().firstOrNull()
        val letter = normalizeChar(firstChar)

        
        if (letter != currentLetter) {
            val headerIndex = sectionedItems.size
            sectionedItems.add(LibraryGridItem.Header(letter, headerCount))
            headerIndices[letter] = headerIndex
            currentLetter = letter
            headerCount++
        }

        
        sectionedItems.add(LibraryGridItem.Media(item))
    }

    return Pair(sectionedItems, headerIndices)
}








fun getAvailableLetters(items: List<MediaItem>): Set<Char> {
    return items
        .map { item ->
            val firstChar = item.name.trim().firstOrNull()
            normalizeChar(firstChar)
        }
        .toSet()
}
