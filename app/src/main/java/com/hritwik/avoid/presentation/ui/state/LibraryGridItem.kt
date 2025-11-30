package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.model.library.MediaItem








sealed class LibraryGridItem {
    






    data class Header(val letter: Char, val index: Int = 0) : LibraryGridItem() {
        
        val id: String = "header_${letter}_$index"
    }

    



    data class Media(val item: MediaItem) : LibraryGridItem() {
        
        val id: String = item.id
    }
}
