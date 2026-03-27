package com.statushub.app.ui.screens.saved

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.statushub.app.R
import com.statushub.app.data.model.SavedStatus
import com.statushub.app.ui.theme.*
import com.statushub.app.ui.viewmodel.SavedViewModel

enum class SavedTab(val title: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    VAULT("Vault")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel(),
    onItemClick: (SavedStatus, Int) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle(initialValue = false)
    
    var showDeleteDialog by remember { mutableStateOf<SavedStatus?>(null) }
    var showVaultLock by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    
    val tabs = listOf(
        SavedTab.ALL to Icons.Outlined.Folder,
        SavedTab.FAVORITES to Icons.Outlined.FavoriteBorder,
        SavedTab.VAULT to Icons.Outlined.Lock
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.saved_title))
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.saved_cancel)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
                
                // Tab row
                TabRow(
                    selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab },
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                tabPositions[tabs.indexOfFirst { it.first == selectedTab }]
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEach { (tab, icon) ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = {
                                if (tab == SavedTab.VAULT && !isPremium) {
                                    showPremiumDialog = true
                                } else if (tab == SavedTab.VAULT && !viewModel.isVaultUnlocked()) {
                                    showVaultLock = true
                                } else {
                                    viewModel.selectTab(tab)
                                }
                            },
                            text = { Text(tab.title) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                selectedTab == SavedTab.ALL && uiState.savedStatuses.isEmpty() -> {
                    EmptyContent(
                        icon = Icons.Outlined.FolderOpen,
                        message = stringResource(R.string.saved_no_items),
                        animationRes = R.raw.empty_state
                    )
                }
                
                selectedTab == SavedTab.FAVORITES && uiState.favoriteStatuses.isEmpty() -> {
                    EmptyContent(
                        icon = Icons.Outlined.FavoriteBorder,
                        message = stringResource(R.string.saved_no_favorites),
                        animationRes = R.raw.empty_state
                    )
                }
                
                selectedTab == SavedTab.VAULT && uiState.hiddenStatuses.isEmpty() -> {
                    EmptyContent(
                        icon = Icons.Outlined.Lock,
                        message = stringResource(R.string.saved_no_vault),
                        animationRes = R.raw.empty_state
                    )
                }
                
                else -> {
                    val items = when (selectedTab) {
                        SavedTab.ALL -> uiState.savedStatuses
                        SavedTab.FAVORITES -> uiState.favoriteStatuses
                        SavedTab.VAULT -> uiState.hiddenStatuses
                    }
                    
                    SavedItemsGrid(
                        items = items,
                        onItemClick = { item, index -> onItemClick(item, index) },
                        onItemLongClick = { item -> showDeleteDialog = item },
                        onFavoriteClick = { item -> viewModel.toggleFavorite(item) }
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { status ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.saved_delete_title)) },
            text = { Text(stringResource(R.string.saved_delete_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStatus(status)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.saved_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.saved_cancel))
                }
            }
        )
    }
    
    // Vault lock dialog
    if (showVaultLock) {
        VaultLockDialog(
            onUnlock = { pin ->
                if (viewModel.unlockVault(pin)) {
                    showVaultLock = false
                    viewModel.selectTab(SavedTab.VAULT)
                }
            },
            onDismiss = { showVaultLock = false }
        )
    }
    
    // Premium dialog
    if (showPremiumDialog) {
        PremiumDialog(
            onDismiss = { showPremiumDialog = false },
            onUpgrade = {
                // Navigate to premium upgrade
                showPremiumDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedItemsGrid(
    items: List<SavedStatus>,
    onItemClick: (SavedStatus, Int) -> Unit,
    onItemLongClick: (SavedStatus) -> Unit,
    onFavoriteClick: (SavedStatus) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            SavedItemCard(
                savedStatus = item,
                onClick = { onItemClick(item, items.indexOf(item)) },
                onLongClick = { onItemLongClick(item) },
                onFavoriteClick = { onFavoriteClick(item) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedItemCard(
    savedStatus: SavedStatus,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Image/Video thumbnail
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(savedStatus.file)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Video indicator
        if (savedStatus.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        // Favorite indicator
        if (savedStatus.isFavorite) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = FavoriteRed,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
        
        // Favorite button overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            SmallIconButton(
                icon = if (savedStatus.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                onClick = onFavoriteClick,
                tint = if (savedStatus.isFavorite) FavoriteRed else Color.White
            )
        }
    }
}

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier.size(32.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    animationRes: Int
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(animationRes)
            )
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )
            
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(120.dp)
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VaultLockDialog(
    onUnlock: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vault_unlock)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.vault_enter_pin),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // PIN input
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(4) { index ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (error) MaterialTheme.colorScheme.errorContainer
                                   else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                if (pin.length > index) {
                                    Text(
                                        text = "●",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = if (error) MaterialTheme.colorScheme.error
                                               else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (error) {
                    Text(
                        text = stringResource(R.string.vault_wrong_pin),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // PIN keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "⌫")
                    ).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { digit ->
                                if (digit.isEmpty()) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            error = false
                                            when (digit) {
                                                "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                else -> if (pin.length < 4) pin += digit
                                            }
                                            if (pin.length == 4) {
                                                onUnlock(pin)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = digit,
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.saved_cancel))
            }
        }
    )
}

@Composable
private fun PremiumDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { 
            Text(
                text = stringResource(R.string.vault_premium),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.vault_premium_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(8.dp))
                
                listOf(
                    stringResource(R.string.premium_feature_1),
                    stringResource(R.string.premium_feature_2),
                    stringResource(R.string.premium_feature_3),
                    stringResource(R.string.premium_feature_4)
                ).forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.premium_title))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.saved_cancel))
            }
        }
    )
}
