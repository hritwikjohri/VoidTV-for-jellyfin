package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class AuthorizeQuickConnectWithTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase<AuthorizeQuickConnectWithTokenUseCase.Params, Unit>(Dispatchers.IO) {

    data class Params(
        val serverUrl: String,
        val code: String,
        val token: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<Unit> {
        return authRepository.authorizeQuickConnectWithToken(
            parameters.serverUrl,
            parameters.code,
            parameters.token
        )
    }
}
