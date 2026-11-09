package com.fauzan.kafe.domain.model

/**
 * Unified reactive UI State representation following Unidirectional Data Flow (UDF).
 * Sealed interface ensures exhaustive pattern matching in Activities/Fragments.
 *
 * @param T data payload type
 */
sealed interface UiState<out T> {

    /**
     * Initial idle state before any action or request begins.
     */
    object Idle : UiState<Nothing>

    /**
     * Asynchronous loading state (e.g. fetching menu, processing checkout).
     */
    object Loading : UiState<Nothing>

    /**
     * Empty state when request succeeded but contains zero records.
     */
    object Empty : UiState<Nothing>

    /**
     * Successful data emission containing the typed payload.
     *
     * @property data the resulting domain model or list
     */
    data class Success<T>(val data: T) : UiState<T>

    /**
     * Error state containing user-friendly message and optional throwable cause.
     *
     * @property message human-readable error description
     * @property cause original exception or throwable
     */
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}
