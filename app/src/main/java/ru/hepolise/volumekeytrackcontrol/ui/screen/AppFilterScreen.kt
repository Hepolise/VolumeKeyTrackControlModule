package ru.hepolise.volumekeytrackcontrol.ui.screen

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.hepolise.volumekeytrackcontrol.ui.debounce
import ru.hepolise.volumekeytrackcontrol.ui.viewmodel.AppIconViewModel
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.BLACK_LIST_APPS
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SETTINGS_PREFS_NAME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.WHITE_LIST_APPS
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getApps
import ru.hepolise.volumekeytrackcontrolmodule.R

private const val MAX_APPS = 100

private val LETTERS = ('A'..'Z').toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterScreen(
    filterType: SharedPreferencesUtil.AppFilterType,
    sharedPreferences: SharedPreferences,
    navController: NavController? = null,
    viewModel: AppIconViewModel = viewModel(),
) {
    val context = LocalContext.current
    val iconMap by viewModel.iconMap.collectAsState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isRefreshing by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    val debouncedQuery by remember(searchQuery) {
        derivedStateOf {
            searchQuery.trim().replace(Regex("\\s+"), " ")
        }.debounce(300, scope)
    }

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val selectedApps = remember { mutableStateListOf<String>() }

    val appState by remember {
        derivedStateOf {
            AppListState(
                allApps = apps,
                selectedPackages = selectedApps.toSet()
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var isSnackbarVisible by remember { mutableStateOf(false) }

    var showClearDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val isScrolling by remember {
        derivedStateOf {
            lazyListState.isScrollInProgress
        }
    }
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            focusManager.clearFocus()
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
        }
    }
    val bottomPadding by animateDpAsState(
        targetValue = if (selectedApps.isNotEmpty()) 56.dp else 0.dp,
        animationSpec = tween(durationMillis = 300)
    )

    val buttonContainerColor = MaterialTheme.colorScheme.primary
    val buttonContentColor = MaterialTheme.colorScheme.onPrimary

    val onRefresh: () -> Unit = {
        isRefreshing = true
        apps = emptyList()
        scope.launch {
            apps = withContext(Dispatchers.IO) {
                getAllApps(context)
            }
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        selectedApps.addAll(sharedPreferences.getApps(filterType))
        onRefresh()
    }

    fun saveApps() {
        sharedPreferences.edit {
            putStringSet(
                when (filterType) {
                    SharedPreferencesUtil.AppFilterType.BlackList -> BLACK_LIST_APPS
                    SharedPreferencesUtil.AppFilterType.WhiteList -> WHITE_LIST_APPS
                    else -> throw IllegalStateException("Invalid filter type: $filterType")
                },
                selectedApps.toSet()
            )
        }
    }

    fun handleAppSelection(packageName: String) {
        if (selectedApps.contains(packageName)) {
            selectedApps.remove(packageName)
        } else {
            if (selectedApps.size >= MAX_APPS) {
                if (!isSnackbarVisible) {
                    scope.launch {
                        isSnackbarVisible = true
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.max_apps_limit,
                                MAX_APPS
                            )
                        )
                        isSnackbarVisible = false
                    }
                }
                return
            } else {
                selectedApps.add(packageName)
            }
        }

        saveApps()
    }

    val filteredApps by remember(appState, debouncedQuery) {
        derivedStateOf {
            appState.allApps
                .asSequence()
                .filter { app ->
                    debouncedQuery.isEmpty() ||
                            app.name.contains(debouncedQuery, ignoreCase = true) ||
                            app.packageName.contains(debouncedQuery, ignoreCase = true)
                }
                .sortedWith(
                    AppListComparator(appState.selectedPackages)
                )
                .toList()
        }
    }

    val letterToIndexMap by remember(filteredApps) {
        derivedStateOf {
            buildMap {
                filteredApps.forEachIndexed { index, app ->
                    if (selectedApps.contains(app.packageName)) {
                        return@forEachIndexed
                    }
                    val firstLetter = app.name.firstOrNull()?.uppercaseChar()
                    if (firstLetter != null && firstLetter !in this) {
                        put(firstLetter, index)
                    }
                }
            }
        }
    }

    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var showLetterPopup by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    activeLetter = null
                }
                focusManager.clearFocus()
                return Offset.Zero
            }
        }
    }


    if (showClearDialog) {
        ClearAppsAlertDialog(
            size = selectedApps.size,
            onConfirm = {
                selectedApps.clear()
                saveApps()
                showClearDialog = false
            },
            onDismissRequest = { showClearDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.select_apps_for,
                            stringResource(filterType.resourceId)
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search_apps)) },
                    enabled = !isRefreshing,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear_search)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .focusable(true)
                )

                Box(modifier = Modifier.weight(1f)) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = bottomPadding),
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(nestedScrollConnection)
                        ) {
                            items(
                                items = filteredApps,
                                key = { it.packageName },
                                contentType = { "App" }
                            ) { app ->
                                val index = filteredApps.indexOf(app)
                                val isFirstUnselected = index > 0 &&
                                        selectedApps.contains(filteredApps[index - 1].packageName) &&
                                        !selectedApps.contains(app.packageName)

                                if (isFirstUnselected) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .animateItem()
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus()
                                            handleAppSelection(app.packageName)
                                        }
                                        .padding(8.dp)
                                ) {
                                    LaunchedEffect(app.packageName) {
                                        viewModel.loadIcon(app.packageName)
                                    }
                                    val icon = iconMap[app.packageName]
                                    AppIcon(icon, app.name)

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = app.name,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Checkbox(
                                        checked = selectedApps.contains(app.packageName),
                                        onCheckedChange = {
                                            focusManager.clearFocus()
                                            handleAppSelection(app.packageName)
                                        }
                                    )
                                }
                            }

                        }
                    }
                }
            }

            AppLetters(activeLetter) { char, showLetter ->
                showLetterPopup = showLetter
                if (char != null && char != activeLetter) {
                    activeLetter = char
                    letterToIndexMap[activeLetter]?.let { index ->
                        scope.launch {
                            lazyListState.animateScrollToItem(index)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedApps.isNotEmpty() && !isRefreshing,
                enter = slideInVertically { height -> height } + fadeIn(),
                exit = slideOutVertically { height -> height } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                AnimatedContent(
                    targetState = isAtBottom,
                    transitionSpec = {
                        val slideDirection = if (targetState)
                            AnimatedContentTransitionScope.SlideDirection.Up else
                            AnimatedContentTransitionScope.SlideDirection.Down

                        slideIntoContainer(slideDirection) + fadeIn() togetherWith
                                slideOutOfContainer(slideDirection) + fadeOut()
                    }
                ) { atBottom ->
                    if (atBottom) {
                        Button(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(48.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonContainerColor,
                                contentColor = buttonContentColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                pluralStringResource(
                                    id = R.plurals.app_filter_clear_apps,
                                    count = selectedApps.size,
                                    selectedApps.size
                                )
                            )
                        }
                    } else {
                        FloatingActionButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.size(48.dp),
                            shape = MaterialTheme.shapes.medium,
                            containerColor = buttonContainerColor,
                            contentColor = buttonContentColor,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            AppLetterPopup(activeLetter, showLetterPopup)
        }
    }
}

@Immutable
private data class AppInfo(
    val name: String,
    val packageName: String
)

private fun getAllApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    return packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES)
        .filter { it.applicationInfo != null }
        .map { packageInfo ->
            AppInfo(
                name = packageInfo.applicationInfo?.loadLabel(packageManager).toString(),
                packageName = packageInfo.packageName
            )
        }
}

@Composable
private fun AppIcon(bitmap: Bitmap?, contentDescription: String) {
    AsyncImage(
        model = bitmap,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}


@Composable
private fun ClearAppsAlertDialog(
    size: Int,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.clear)) },
        text = {
            Text(
                pluralStringResource(
                    id = R.plurals.app_filter_clear_message,
                    count = size,
                    size
                )
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.no))
            }
        }
    )
}

@Composable
private fun BoxScope.AppLetters(
    activeLetter: Char?,
    onActiveLetterChange: (Char?, Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val minTotalHeight = with(density) { LETTERS.size * 24.dp.roundToPx() }

    var containerHeight by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .onSizeChanged { containerHeight = it.height }
    ) {
        if (containerHeight >= minTotalHeight) {

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .pointerInput(Unit) {
                        fun Offset.findLetter() =
                            LETTERS[(this.y / (size.height / LETTERS.size.toFloat())).toInt()
                                .coerceIn(0, LETTERS.size - 1)]
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                onActiveLetterChange(offset.findLetter(), true)
                            },
                            onVerticalDrag = { change, dragAmount ->
                                onActiveLetterChange(change.position.findLetter(), true)
                                change.consume()
                            },
                            onDragEnd = { onActiveLetterChange(null, false) },
                            onDragCancel = { onActiveLetterChange(null, false) }
                        )
                    }
            ) {
                LETTERS.forEach { letter ->
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (letter == activeLetter) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(vertical = 1.dp)
                            .clickable { onActiveLetterChange(letter, false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.AppLetterPopup(
    activeLetter: Char?,
    showLetterPopup: Boolean
) {
    var popupScale by remember { mutableFloatStateOf(1f) }
    AnimatedVisibility(
        visible = showLetterPopup && activeLetter != null,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = scaleOut() + fadeOut(),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Surface(
            modifier = Modifier
                .size(80.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(100f, 100f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                if (activeLetter != null) {
                    Text(
                        text = activeLetter.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.scale(popupScale)
                    )
                }
            }
        }
    }
    LaunchedEffect(activeLetter) {
        if (activeLetter != null) {
            popupScale = 1.2f
            delay(100)
            popupScale = 1f
        }
    }
}

private data class AppListState(
    val allApps: List<AppInfo>,
    val selectedPackages: Set<String>
)

private class AppListComparator(
    private val selectedPackages: Set<String>
) : Comparator<AppInfo> {
    override fun compare(a: AppInfo, b: AppInfo): Int {
        val aSelected = a.packageName in selectedPackages
        val bSelected = b.packageName in selectedPackages

        return when {
            aSelected && !bSelected -> -1
            !aSelected && bSelected -> 1
            else -> a.name.compareTo(b.name, ignoreCase = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppFilterScreen() {
    AppFilterScreen(
        filterType = SharedPreferencesUtil.AppFilterType.WhiteList,
        sharedPreferences = LocalContext.current.getSharedPreferences(
            SETTINGS_PREFS_NAME,
            Context.MODE_PRIVATE,
        ),
    )
}