package com.hritwik.avoid.domain.usecase.library

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.Studio
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.utils.constants.ApiConstants
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetStudiosUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetStudiosUseCase.Params, List<Studio>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val accessToken: String,
        val libraryId: String,
        val limit: Int = 10,
        val includeItemTypes: String = ApiConstants.ITEM_TYPE_SERIES
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<Studio>> {
        return libraryRepository.getStudios(
            userId = parameters.userId,
            accessToken = parameters.accessToken,
            libraryId = parameters.libraryId,
            limit = parameters.limit,
            includeItemTypes = parameters.includeItemTypes
        )
    }
}
