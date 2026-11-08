package com.lernspace.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        // Left Landscape Compact Navigation Rail (80dp - saves space & avoids status bar collision)
        Surface(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight(),
            color = LernspaceSurfaceCard,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lernspace",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Nav Items
                    Screen.items.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { onNavigate(screen.route) },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                // Bottom Section: Stylus / Quick Capture & Profile
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Quick Canvas Floating Action Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentBlueContainer,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onQuickAdd() },
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Create,
                                contentDescription = "Quick Canvas",
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = LernspaceBorder,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // User Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LernspaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                content()
            }
        }
    }
}

