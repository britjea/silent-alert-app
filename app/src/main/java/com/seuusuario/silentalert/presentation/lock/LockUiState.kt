package com.seuusuario.silentalert.presentation.lock

sealed class LockUiState {
    object Idle : LockUiState()
    object Unlocking : LockUiState()
    object UnlockNormal : LockUiState()
}
