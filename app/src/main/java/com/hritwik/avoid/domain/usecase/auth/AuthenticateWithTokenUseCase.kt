package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.model.auth.Server
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class AuthenticateWithTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase<AuthenticateWithTokenUseCase.Params, AuthSession>(Dispatchers.IO) {

    data class Params(
        val server: Server,
        val mediaToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<AuthSession> {
        val result = authRepository.authenticateWithToken(parameters.server, parameters.mediaToken)
        if (result is NetworkResult.Success) {
            authRepository.saveAuthSession(result.data)
        }
        return result
    }
}
