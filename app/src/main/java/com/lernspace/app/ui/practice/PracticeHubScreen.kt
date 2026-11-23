package com.lernspace.app.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lernspace.app.navigation.keepAliveTab
import com.lernspace.app.ui.banksoal.BankSoalScreen
import com.lernspace.app.ui.flashcards.FlashcardsScreen
import com.lernspace.app.ui.quiz.QuizScreen
import com.lernspace.app.ui.theme.LernspaceDark
import com.lernspace.app.ui.theme.LernspaceSurface
import com.lernspace.app.ui.theme.LernspaceSurfaceCard
import com.lernspace.app.ui.theme.LocalIsDarkTheme

enum class PracticeTab(val label: String, val icon: ImageVector) {
    FLASHCARDS("Flashcards", Icons.Filled.Style),
    QUIZ("Kuis Interaktif", Icons.Filled.Quiz),
    BANK_SOAL("Bank Soal", Icons.Filled.FolderCopy)
}

object PracticeHubState {
    var selectedTab by mutableStateOf(PracticeTab.FLASHCARDS)
}

@Composable
fun PracticeHubScreen(
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(PracticeHubState.selectedTab) }
    val isDark = LocalIsDarkTheme.current

    LaunchedEffect(PracticeHubState.selectedTab) {
        activeTab = PracticeHubState.selectedTab
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LernspaceSurface)
    ) {
        // Modern Pill Segmented Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LernspaceSurfaceCard,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Practice Hub",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )
                    Text(
                        text = "•  Latihan & Evaluasi",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Segmented Switcher Chips
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF1E2430) else Color(0xFFF1F3F5),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PracticeTab.values().forEach { tab ->
                            val isSelected = activeTab == tab
                            val chipBg = if (isSelected) {
                                if (isDark) Color.White else Color.Black
                            } else {
                                Color.Transparent
                            }
                            val chipFg = if (isSelected) {
                                if (isDark) Color.Black else Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(chipBg)
                                    .clickable {
                                        activeTab = tab
                                        PracticeHubState.selectedTab = tab
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = chipFg,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = chipFg
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tab Content with Instant Keep-Alive
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(activeTab == PracticeTab.FLASHCARDS)
            ) {
                FlashcardsScreen()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(activeTab == PracticeTab.QUIZ)
            ) {
                QuizScreen()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .keepAliveTab(activeTab == PracticeTab.BANK_SOAL)
            ) {
                BankSoalScreen(
                    onNavigateToQuiz = {
                        activeTab = PracticeTab.QUIZ
                        PracticeHubState.selectedTab = PracticeTab.QUIZ
                    }
                )
            }
        }
    }
}
