package com.example.dailysnapshot.di

import com.example.dailysnapshot.viewmodels.CameraViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { CameraViewModel() }
}