package com.statushub.app.ui.screens.vault

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.statushub.app.R
import com.statushub.app.data.model.SavedStatus
import com.statushub.app.ui.theme.*
import com.statushub.app.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onItemClick: (SavedStatus, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    var showSetupDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<SavedStatus?>(null) }

    LaunchedEffect(Unit) {
        viewModel.checkVaultStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.vault_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.saved_cancel)
                        )
                    }
                },
                actions = {
                    if (!isLocked) {
                        IconButton(onClick = { viewModel.lockVault() }) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Lock Vault"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLocked -> {
                    VaultLockScreen(
                        onUnlock = { pin ->
                            viewModel.unlockVault(pin)
                        },
                        onSetupPin = {
                            showSetupDialog = true
                        }
                    )
                }
                
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                uiState.hiddenStatuses.isEmpty() -> {
                    EmptyVaultContent()
                }
                
                else -> {
                    VaultItemsGrid(
                        items = uiState.hiddenStatuses,
                        onItemClick = { item, index -> onItemClick(item, index) },
                        onItemLongClick = { item -> showDeleteDialog = item },
                        onMoveOut = { item -> viewModel.moveToRegular(item) }
                    )
                }
            }
        }
    }

    // PIN setup dialog
    if (showSetupDialog) {
        PinSetupDialog(
            onDismiss = { showSetupDialog = false },
            onSet = { pin ->
                viewModel.setupPin(pin)
                showSetupDialog = false
            }
        )
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
}

@Composable
private fun VaultLockScreen(
    onUnlock: (String) -> Unit,
    onSetupPin: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showSetup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.vault_locked),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.vault_enter_pin),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(32.dp))
        
        // PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) { index ->
                Surface(
                    shape = CircleShape,
                    color = if (error) MaterialTheme.colorScheme.errorContainer
                           else if (pin.length > index) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(20.dp)
                ) {}
            }
        }
        
        if (error) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.vault_wrong_pin),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Keypad
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            ).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { digit ->
                        if (digit.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    error = false
                                    when (digit) {
                                        "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        else -> if (pin.length < 4) {
                                            pin += digit
                                            if (pin.length == 4) {
                                                onUnlock(pin)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                shape = RoundedCornerShape(16.dp)
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
        
        Spacer(Modifier.height(24.dp))
        
        TextButton(onClick = onSetupPin) {
            Text(stringResource(R.string.vault_setup))
        }
    }
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (step == 1) stringResource(R.string.vault_setup) else stringResource(R.string.vault_confirm_pin)) 
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (step == 1) stringResource(R.string.vault_setup_desc) else "Confirm your PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                // PIN dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(4) { index ->
                        Surface(
                            shape = CircleShape,
                            color = if (if (step == 1) pin.length > index else confirmPin.length > index) 
                                    MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(20.dp)
                        ) {}
                    }
                }
                
                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Keypad
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { digit ->
                                if (digit.isEmpty()) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            error = ""
                                            val currentPin = if (step == 1) pin else confirmPin
                                            when (digit) {
                                                "⌫" -> {
                                                    if (currentPin.isNotEmpty()) {
                                                        if (step == 1) pin = pin.dropLast(1)
                                                        else confirmPin = confirmPin.dropLast(1)
                                                    }
                                                }
                                                else -> {
                                                    if (currentPin.length < 4) {
                                                        val newPin = currentPin + digit
                                                        if (step == 1) {
                                                            pin = newPin
                                                            if (newPin.length == 4) {
                                                                step = 2
                                                            }
                                                        } else {
                                                            confirmPin = newPin
                                                            if (newPin.length == 4) {
                                                                if (pin == confirmPin) {
                                                                    onSet(pin)
                                                                } else {
                                                                    error = "PINs don't match"
                                                                    confirmPin = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp),
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
private fun EmptyVaultContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.saved_no_vault),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Move items to the vault to keep them private",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultItemsGrid(
    items: List<SavedStatus>,
    onItemClick: (SavedStatus, Int) -> Unit,
    onItemLongClick: (SavedStatus) -> Unit,
    onMoveOut: (SavedStatus) -> Unit
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
            VaultItemCard(
                savedStatus = item,
                onClick = { onItemClick(item, items.indexOf(item)) },
                onLongClick = { onItemLongClick(item) },
                onMoveOut = { onMoveOut(item) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultItemCard(
    savedStatus: SavedStatus,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoveOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
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
        
        // Move out button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            ) {
                IconButton(
                    onClick = onMoveOut,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = "Move out of vault",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
