package com.statushub.app.ui.screens.preview

import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.statushub.app.R
import com.statushub.app.data.model.Status
import com.statushub.app.data.model.SavedStatus
import com.statushub.app.data.model.StatusType
import com.statushub.app.ui.theme.*
import com.statushub.app.ui.viewmodel.PreviewViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewScreen(
    viewModel: PreviewViewModel = hiltViewModel(),
    statusId: String,
    isSavedStatus: Boolean,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showControls by remember { mutableStateOf(true) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { uiState.items.size }
    )
    
    val scope = rememberCoroutineScope()
    
    // Animate controls visibility
    val controlsAlpha by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f,
        animationSpec = tween(200),
        label = "controlsAlpha"
    )

    LaunchedEffect(statusId) {
        viewModel.loadItems(statusId, isSavedStatus)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Status pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp
        ) { page ->
            val item = uiState.items.getOrNull(page)
            
            when {
                item is Status && item.isVideo -> {
                    VideoPlayer(
                        status = item,
                        isPlaying = page == pagerState.currentPage && showControls.not(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                item is SavedStatus && item.isVideo -> {
                    VideoPlayer(
                        savedStatus = item,
                        isPlaying = page == pagerState.currentPage && showControls.not(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                item is Status && item.isImage -> {
                    StatusImage(
                        status = item,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                item is SavedStatus && item.isImage -> {
                    StatusImage(
                        savedStatus = item,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Tap to toggle controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls }
                    )
                }
        )

        // Top bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.saved_cancel),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Page indicator
                if (uiState.items.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${uiState.items.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }

        // Bottom controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save button
                    if (!isSavedStatus) {
                        ControlButton(
                            icon = if (showSaveConfirmation) Icons.Filled.Check else Icons.Filled.Save,
                            label = if (showSaveConfirmation) stringResource(R.string.preview_saved) else stringResource(R.string.preview_save),
                            onClick = {
                                val currentItem = uiState.items.getOrNull(pagerState.currentPage)
                                if (currentItem is Status) {
                                    viewModel.saveStatus(currentItem)
                                    showSaveConfirmation = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        showSaveConfirmation = false
                                    }
                                }
                            },
                            isHighlighted = showSaveConfirmation,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Favorite button
                    ControlButton(
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        label = stringResource(R.string.preview_favorite),
                        onClick = {
                            isFavorite = !isFavorite
                            val currentItem = uiState.items.getOrNull(pagerState.currentPage)
                            if (currentItem is SavedStatus) {
                                viewModel.toggleFavorite(currentItem)
                            }
                        },
                        isHighlighted = isFavorite,
                        tint = if (isFavorite) FavoriteRed else Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    // Share button
                    ControlButton(
                        icon = Icons.Filled.Share,
                        label = stringResource(R.string.preview_share),
                        onClick = {
                            val currentItem = uiState.items.getOrNull(pagerState.currentPage)
                            currentItem?.let { viewModel.shareItem(it) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    tint: Color = Color.White
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isHighlighted) Color.White else tint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun StatusImage(
    status: Status? = null,
    savedStatus: SavedStatus? = null,
    modifier: Modifier = Modifier
) {
    val file = status?.file ?: savedStatus?.file
    
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(file)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun VideoPlayer(
    status: Status? = null,
    savedStatus: SavedStatus? = null,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = status?.file ?: savedStatus?.file
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            file?.let {
                val mediaItem = MediaItem.fromUri(it.absolutePath)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = false
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                controllerShowTimeoutMs = 3000
                keepScreenOn = true
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
