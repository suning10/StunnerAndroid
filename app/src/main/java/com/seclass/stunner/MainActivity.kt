package com.seclass.stunner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.seclass.stunner.detection.SimpleGoalDetector
import com.seclass.stunner.detection.TFLiteBallDetector
import com.seclass.stunner.detection.TrajectoryGoalDetector
import com.seclass.stunner.navigation.AppNavigation
import com.seclass.stunner.repository.*
import com.seclass.stunner.ui.theme.StunnerTheme
import com.seclass.stunner.viewmodel.DetectionViewModel
import com.seclass.stunner.viewmodel.StatsViewModel

class MainActivity : ComponentActivity() {

    // Dependency graph (manual DI — replace with Hilt later if needed)
    private val db by lazy { AppDatabase.getInstance(this) }
    private val goalEventRepo by lazy { GoalEventRepository(db.goalEventDao()) }
    private val settingsRepo by lazy { SettingsRepository(this) }
    private val apiRepo by lazy { ApiRepository(settingsRepo) }
    private val detector by lazy { TFLiteBallDetector().also { it.initialize(this) } }

    // ← swap between SimpleGoalDetector() and TrajectoryGoalDetector() based on test performance
    //private val goalDetector by lazy { SimpleGoalDetector() }
    private val goalDetector by lazy { TrajectoryGoalDetector() }

    private val detectionRepo by lazy { DetectionRepository(detector, goalDetector, goalEventRepo, apiRepo) }

    private val detectionViewModel: DetectionViewModel by viewModels {
        DetectionViewModel.factory(detectionRepo)
    }
    private val statsViewModel: StatsViewModel by viewModels {
        StatsViewModel.factory(goalEventRepo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.i("Stunner/Main", "onCreate — app started")
        setContent {
            StunnerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        detectionViewModel = detectionViewModel,
                        statsViewModel = statsViewModel
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            detector.release()
        }
    }
}
