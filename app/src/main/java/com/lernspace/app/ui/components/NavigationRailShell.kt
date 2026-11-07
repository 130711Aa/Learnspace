package com.lernspace.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lernspace.app.navigation.Screen
import com.lernspace.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletAppShell(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onQuickAdd: () -> Unit,
    onStartFocusSession: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LernspaceSurface)
    ) {
        // Left Landscape Navigation Rail (72dp - 200dp responsive)
        Surface(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight(),
            color = LernspaceSurfaceCard,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Logo
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LernspaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoStories,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Lernspace",
                                style = MaterialTheme.typography.titleLarge,
                                color = LernspaceDark
                            )
                            Text(
                                text = "Learning OS",
                                style = MaterialTheme.typography.labelSmall,
                                color = LernspaceTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Nav Items
                    Screen.items.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { onNavigate(screen.route) },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = LernspaceDark,
                                indicatorColor = LernspaceDark,
                                unselectedIconColor = LernspaceTextSecondary,
                                unselectedTextColor = LernspaceTextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // Bottom Section: Stylus / Quick Capture & Profile
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onQuickAdd,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Create,
                            contentDescription = "Quick Canvas",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Canvas",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    HorizontalDivider(color = LernspaceBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LernspaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "U",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Column {
                            Text(
                                text = "Student Workspace",
                                style = MaterialTheme.typography.labelMedium,
                                color = LernspaceTextPrimary
                            )
                            Text(
                                text = "Local-first Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentTeal
                            )
                        }
                    }
                }
            }
        }

        // Main Content Area with Tablet TopBar
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Tablet TopBar
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Active Term",
                            style = MaterialTheme.typography.labelMedium,
                            color = LernspaceTextSecondary
                        )
                        Text(
                            text = "/",
                            color = LernspaceBorder
                        )
                        Text(
                            text = "Sprint Horizon",
                            style = MaterialTheme.typography.labelMedium,
                            color = LernspaceTextPrimary
                        )
                    }
                },
                actions = {
                    // Stylus Status Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LernspaceSurfaceLow,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Stylus Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = LernspaceTextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentTeal)
                            )
                        }
                    }

                    IconButton(onClick = { /* Search */ }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = LernspaceTextSecondary
                        )
                    }

                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = LernspaceTextSecondary
                        )
                    }

                    Button(
                        onClick = onStartFocusSession,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LernspaceDark
                        ),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Start Focus",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Focus Session",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LernspaceSurface
                )
            )

            // Screen content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                content()
            }
        }
    }
}
