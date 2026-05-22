package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.data.api.ErrorMode
import com.example.data.api.FriendlyError
import com.example.data.api.NetworkSimulationManager
import com.example.data.model.PagingItem
import com.example.data.model.SourceType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lazyPagingItems = viewModel.pagingItemsFlow.collectAsLazyPagingItems()
    val initialOffset by viewModel.initialOffset.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Dialog & UI interaction states
    var showEditDialog by remember { mutableStateOf<PagingItem?>(null) }
    var showInsertDialog by remember { mutableStateOf(false) }
    var showInsertAfterDialog by remember { mutableStateOf<PagingItem?>(null) }
    var currentTab by remember { mutableStateOf(0) } // 0: Live Feed, 1: DB Logs, 2: Info

    // Centralized simulation state in Compose to sync with UI switches
    var selectedErrorMode by remember { mutableStateOf(NetworkSimulationManager.activeErrorMode) }

    // Theme colors matching premium slate-themed design
    val primaryColor = Color(0xFF6366F1) // Indigo accent
    val primaryLight = Color(0xFFEEF2FF)
    val secondaryColor = Color(0xFF4F46E5)
    val accentColor = Color(0xFFEC4899) // Pink accent
    val backgroundColor = Color(0xFFF8FAFC) // Cool grey
    val cardBackground = Color.White

    // Listen for outside simulated updates (for state cohesion)
    LaunchedEffect(Unit) {
        NetworkSimulationManager.onErrorModeChanged = { mode ->
            selectedErrorMode = mode
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo symbol
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Core Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Floating Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillBadge(text = "Paging 3.3", containerColor = primaryLight, textColor = secondaryColor)
                        PillBadge(text = "Retrofit OK", containerColor = Color(0xFFECFDF5), textColor = Color(0xFF059669))
                        PillBadge(text = "Backoff Retry", containerColor = Color(0xFFFFF7ED), textColor = Color(0xFFD97706))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Paging Architecture",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Advanced Bidirectional Pagination & Network Error Recovery Flow",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Feed") },
                    label = { Text("Feed", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = primaryLight
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Console Logs") },
                    label = { Text("Log Console", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = primaryLight
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Info") },
                    label = { Text("Architecture", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = primaryLight
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(
                    onClick = { showInsertDialog = true },
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_add_item")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Insert Dynamic Item")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> {
                    // TAB 0: ADVANCED PAGING FEED
                    
                    // Live Network Error Simulation Hub Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedErrorMode == ErrorMode.NONE) Color(0xFF10B981) else Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Error Simulation Control Hub",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Simulate active cloud server disruption. When enabled, paging requests will execute exponential backoff retries and elegantly trigger state notifications.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Horizontal grid of simulated conditions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ErrorSelectorChip(
                                    text = "Normal",
                                    selected = selectedErrorMode == ErrorMode.NONE,
                                    onClick = {
                                        NetworkSimulationManager.setMode(ErrorMode.NONE)
                                        selectedErrorMode = ErrorMode.NONE
                                        lazyPagingItems.retry()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ErrorSelectorChip(
                                    text = "Offline",
                                    selected = selectedErrorMode == ErrorMode.NO_INTERNET,
                                    onClick = {
                                        NetworkSimulationManager.setMode(ErrorMode.NO_INTERNET)
                                        selectedErrorMode = ErrorMode.NO_INTERNET
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ErrorSelectorChip(
                                    text = "500 Err",
                                    selected = selectedErrorMode == ErrorMode.SERVER_ERROR_500,
                                    onClick = {
                                        NetworkSimulationManager.setMode(ErrorMode.SERVER_ERROR_500)
                                        selectedErrorMode = ErrorMode.SERVER_ERROR_500
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ErrorSelectorChip(
                                    text = "429 Limit",
                                    selected = selectedErrorMode == ErrorMode.RATE_LIMIT_429,
                                    onClick = {
                                        NetworkSimulationManager.setMode(ErrorMode.RATE_LIMIT_429)
                                        selectedErrorMode = ErrorMode.RATE_LIMIT_429
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ErrorSelectorChip(
                                    text = "Timeout",
                                    selected = selectedErrorMode == ErrorMode.TIMEOUT,
                                    onClick = {
                                        NetworkSimulationManager.setMode(ErrorMode.TIMEOUT)
                                        selectedErrorMode = ErrorMode.TIMEOUT
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Starting position midpoint deck
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Starting Database Index Anchor",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "Offset: $initialOffset",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryColor,
                                    modifier = Modifier
                                        .background(primaryLight, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    value = initialOffset.toFloat(),
                                    onValueChange = { viewModel.setMiddleOffset(it.toInt()) },
                                    valueRange = 0f..250f,
                                    steps = 25,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = primaryColor,
                                        activeTrackColor = primaryColor,
                                        inactiveTrackColor = Color(0xFFE2E8F0)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { 
                                        viewModel.resetFilters()
                                        lazyPagingItems.refresh() 
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reset all state", fontSize = 11.sp, color = primaryColor)
                                }
                            }
                        }
                    }

                    // Central Grid Feed Area with custom load state layouts
                    Box(modifier = Modifier.weight(1f)) {
                        
                        // Scenario 1: Initial load Refresh is loading
                        if (lazyPagingItems.loadState.refresh is LoadState.Loading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(38.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Contacting remote cluster api node...", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                        
                        // Scenario 2: Initial refresh failed with a network error
                        else if (lazyPagingItems.loadState.refresh is LoadState.Error) {
                            val error = (lazyPagingItems.loadState.refresh as LoadState.Error).error
                            FullBleedErrorRepresentation(
                                error = error,
                                onRetryClick = { lazyPagingItems.retry() }
                            )
                        } 
                        
                        // Scenario 3: Empty List
                        else if (lazyPagingItems.itemCount == 0) {
                            EmptyFeedState()
                        } 
                        
                        // Scenario 4: Successful list render displaying elements with bidirectional streams
                        else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("feed_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Backward Page Prepends
                                if (lazyPagingItems.loadState.prepend is LoadState.Loading) {
                                    item {
                                        LoadingIndicatorRow(text = "Unpacking preceding history database items...")
                                    }
                                } else if (lazyPagingItems.loadState.prepend is LoadState.Error) {
                                    val err = (lazyPagingItems.loadState.prepend as LoadState.Error).error
                                    item {
                                        BackwardErrorBanner(error = err, onRetryClick = { lazyPagingItems.retry() })
                                    }
                                }

                                // Anchor point indicator row
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE2E8F0)))
                                        Text(
                                            text = " Initializing anchor point index $initialOffset ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryColor.copy(alpha = 0.7f)
                                        )
                                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE2E8F0)))
                                    }
                                }

                                // Dynamic paginated feeds array list
                                items(
                                    count = lazyPagingItems.itemCount,
                                    key = { index ->
                                        val item = lazyPagingItems[index]
                                        item?.id ?: index
                                    }
                                ) { index ->
                                    val item = lazyPagingItems[index]
                                    if (item != null) {
                                        PagingItemRow(
                                            item = item,
                                            onEditClick = { showEditDialog = item },
                                            onDeleteClick = { idVal ->
                                                viewModel.deleteItemFromDatabase(idVal)
                                            },
                                            onInsertAfterClick = { targetItem ->
                                                showInsertAfterDialog = targetItem
                                            }
                                        )
                                    }
                                }

                                // Forward Page Appends (loading bottom records on scroll)
                                if (lazyPagingItems.loadState.append is LoadState.Loading) {
                                    item {
                                        LoadingIndicatorRow(text = "Requesting forward sequence database records...")
                                    }
                                } else if (lazyPagingItems.loadState.append is LoadState.Error) {
                                    val err = (lazyPagingItems.loadState.append as LoadState.Error).error
                                    item {
                                        ForwardErrorBanner(error = err, onRetryClick = { lazyPagingItems.retry() })
                                    }
                                }
                            }
                        }
                    }
                }
                
                1 -> {
                    // TAB 1: LOGGER
                    LogWidgetConsole(logs = logs)
                }
                
                2 -> {
                    // TAB 2: TECH STACK HELP OVERVIEW
                    TechStackDetails(primaryColor = primaryColor, secondaryColor = secondaryColor, accentColor = accentColor)
                }
            }
        }
    }

    // Dialogue Overlay Screens
    showEditDialog?.let { item ->
        var editTitle by remember { mutableStateOf(item.title) }
        var editSubtitle by remember { mutableStateOf(item.subtitle) }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.editItemInDatabase(item.id, editTitle, editSubtitle)
                        showEditDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Modify Entity State")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Abort")
                }
            },
            title = { Text("Update Database Object #${item.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Direct index modification updates both local flow models and centralized cache repository.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Record Title") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_title_field")
                    )
                    OutlinedTextField(
                        value = editSubtitle,
                        onValueChange = { editSubtitle = it },
                        label = { Text("Details / Specs") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    if (showInsertDialog) {
        var insertId by remember { mutableStateOf((301..999).random().toString()) }
        var insertTitle by remember { mutableStateOf("Fresh Append Record") }
        var insertSubtitle by remember { mutableStateOf("Dynamic database insertion completed.") }
        var positionMode by remember { mutableStateOf(0) } // 0: Prepend (start), 1: Append (end), 2: Choose Index
        var listIndexText by remember { mutableStateOf("120") }

        AlertDialog(
            onDismissRequest = { showInsertDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val idVal = insertId.toIntOrNull() ?: (301..999).random()
                        when (positionMode) {
                            0 -> viewModel.prependItemToDatabase(idVal, insertTitle, insertSubtitle)
                            1 -> viewModel.appendItemToDatabase(idVal, insertTitle, insertSubtitle)
                            2 -> {
                                val idxVal = listIndexText.toIntOrNull() ?: 120
                                viewModel.insertItemAtDatabaseIndex(idxVal, idVal, insertTitle, insertSubtitle)
                            }
                        }
                        showInsertDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.testTag("dialog_confirm_add")
                ) {
                    Text("Insert Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsertDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Insert Extra Dynamic Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = insertId,
                            onValueChange = { insertId = it },
                            label = { Text("ID") },
                            modifier = Modifier.weight(1f).testTag("dialog_id_field")
                        )
                        OutlinedTextField(
                            value = insertTitle,
                            onValueChange = { insertTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.weight(2f).testTag("dialog_title_field")
                        )
                    }
                    OutlinedTextField(
                        value = insertSubtitle,
                        onValueChange = { insertSubtitle = it },
                        label = { Text("Subtitle Specs") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Placement Position:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { positionMode = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = if (positionMode == 0) primaryColor else primaryLight),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Prepend [0]", fontSize = 10.sp, color = if (positionMode == 0) Color.White else secondaryColor)
                        }
                        Button(
                            onClick = { positionMode = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = if (positionMode == 1) primaryColor else primaryLight),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Append [End]", fontSize = 10.sp, color = if (positionMode == 1) Color.White else secondaryColor)
                        }
                        Button(
                            onClick = { positionMode = 2 },
                            colors = ButtonDefaults.buttonColors(containerColor = if (positionMode == 2) primaryColor else primaryLight),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Index Shift", fontSize = 10.sp, color = if (positionMode == 2) Color.White else secondaryColor)
                        }
                    }

                    if (positionMode == 2) {
                        OutlinedTextField(
                            value = listIndexText,
                            onValueChange = { listIndexText = it },
                            label = { Text("Relative Table Offset Index") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        )
    }

    showInsertAfterDialog?.let { target ->
        var insertId by remember { mutableStateOf((400..999).random().toString()) }
        var insertTitle by remember { mutableStateOf("Item below #${target.id}") }
        var insertSubtitle by remember { mutableStateOf("Inserted precisely in table position relative to parent.") }

        AlertDialog(
            onDismissRequest = { showInsertAfterDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        val idVal = insertId.toIntOrNull() ?: (400..999).random()
                        viewModel.insertItemAfter(target.id, idVal, insertTitle, insertSubtitle)
                        showInsertAfterDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Insert Here")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsertAfterDialog = null }) {
                    Text("Dismiss")
                }
            },
            title = { Text("Insert Item Below #${target.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Calculates the dynamic physical index of current node and shifts sequential records by executing a positional write.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = insertId,
                            onValueChange = { insertId = it },
                            label = { Text("New Item ID") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = insertTitle,
                            onValueChange = { insertTitle = it },
                            label = { Text("New Item Title") },
                            modifier = Modifier.weight(2f)
                        )
                    }
                    OutlinedTextField(
                        value = insertSubtitle,
                        onValueChange = { insertSubtitle = it },
                        label = { Text("Details & Specs") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

@Composable
fun ErrorSelectorChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF475569)
        )
    }
}

@Composable
fun PagingItemRow(
    item: PagingItem,
    onEditClick: () -> Unit,
    onDeleteClick: (Int) -> Unit,
    onInsertAfterClick: (PagingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = when (item.type) {
        SourceType.REMOTE -> Color(0xFF6366F1)
        SourceType.LOCAL -> Color(0xFF10B981)
        SourceType.INSERTED -> Color(0xFFF59E0B)
        SourceType.MODIFIED -> Color(0xFFEF4444)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .testTag("item_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.type.name,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Category",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = item.category,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Operations Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp).testTag("edit_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit record",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Insert after
                IconButton(
                    onClick = { onInsertAfterClick(item) },
                    modifier = Modifier.size(36.dp).testTag("insert_insertAfter_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Insert custom element directly below",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Delete
                IconButton(
                    onClick = { onDeleteClick(item.id) },
                    modifier = Modifier.size(36.dp).testTag("delete_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FullBleedErrorRepresentation(
    error: Throwable,
    onRetryClick: () -> Unit
) {
    val friendly = FriendlyError.from(error)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(2.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF2F2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = friendly.title,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = friendly.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = friendly.description,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Network Transaction", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun BackwardErrorBanner(error: Throwable, onRetryClick: () -> Unit) {
    val friendly = FriendlyError.from(error)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border = BorderStroke(1.dp, Color(0xFFFEE2E2))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${friendly.title}. (Scrolling up failed)",
                fontSize = 11.sp,
                color = Color(0xFFB91C1C),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onRetryClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun ForwardErrorBanner(error: Throwable, onRetryClick: () -> Unit) {
    val friendly = FriendlyError.from(error)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border = BorderStroke(1.dp, Color(0xFFFEE2E2))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${friendly.title}. (Scrolling down failed)",
                fontSize = 11.sp,
                color = Color(0xFFB91C1C),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onRetryClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun StatCard(
    letter: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEF2FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )
        }
    }
}

@Composable
fun PillBadge(
    text: String,
    containerColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun LoadingIndicatorRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.8.dp, color = Color(0xFF6366F1))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 11.sp, color = Color(0xFF64748B))
    }
}

@Composable
fun LogWidgetConsole(logs: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SYSTEM EVENT LOGS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("Interceptors Active", color = Color(0xFF64748B), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(10.dp))

            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No events captured yet. Modify data, scroll, or toggle error simulations!", color = Color(0xFF475569), fontSize = 12.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs.size) { index ->
                        Text(
                            text = logs[index],
                            color = if (logs[index].contains("Failure") || logs[index].contains("Retry")) Color(0xFFF87171) else if (logs[index].contains("Success")) Color(0xFF34D399) else Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFeedState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Empty",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("No active records found", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.height(4.dp))
        Text("Verify search parameters or click the Floating Action Button in the bottom right corner to prepend mock values.", color = Color(0xFF64748B), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun TechStackDetails(
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Stack & Resilience Engineering", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Highly polished architectural design implementing fully automated handling, recovery, and mutations:", fontSize = 12.sp, color = Color(0xFF64748B))
        }

        item {
            TechItemCard(
                title = "OkHttp Mock Interceptor",
                desc = "Decoupled interceptor architecture mimicking physical cloud API endpoints. Intercepts standard local/remote request cycles, checks simulation switches, and returns native 500/429/Network drop responses.",
                color = primaryColor
            )
        }
        item {
            TechItemCard(
                title = "Centralized FriendlyError Engine",
                desc = "Unified mapper resolving various network exceptions (java.io.IOException, SocketTimeoutException, HttpException) into clean Material Design explanations to guide user recovery.",
                color = Color(0xFFEF4444)
            )
        }
        item {
            TechItemCard(
                title = "Exponential Backoff Runner",
                desc = "Automated retry mechanism utilizing customizable variables. Safely re-issues requests in case of network fluctuations across incremental time steps (e.g. 1s, 2s, 4s).",
                color = Color(0xFFF59E0B)
            )
        }
        item {
            TechItemCard(
                title = "Bidirectional Paging Multi-State Adapter",
                desc = "Supports scroll-down loading (Appending) and scroll-up history reloading (Prepending) simultaneously. Triggers localized error screens for append/prepend failures separately.",
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun TechItemCard(title: String, desc: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
                    .padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = desc, fontSize = 12.sp, color = Color(0xFF64748B), lineHeight = 16.sp)
            }
        }
    }
}
