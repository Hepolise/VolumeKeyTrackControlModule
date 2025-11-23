package ru.hepolise.volumekeytrackcontrol.ui.screen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.hepolise.volumekeytrackcontrol.ui.component.AppFilterSetting
import ru.hepolise.volumekeytrackcontrol.ui.component.LongPressSetting
import ru.hepolise.volumekeytrackcontrol.ui.component.LongPressSettingData
import ru.hepolise.volumekeytrackcontrol.ui.component.SwapButtonsSetting
import ru.hepolise.volumekeytrackcontrol.ui.component.VibrationEffectSetting
import ru.hepolise.volumekeytrackcontrol.ui.component.VibrationSettingData
import ru.hepolise.volumekeytrackcontrol.ui.isInstalledAfterReboot
import ru.hepolise.volumekeytrackcontrol.util.AppFilterType
import ru.hepolise.volumekeytrackcontrol.util.Constants
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.APP_FILTER_TYPE_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.EFFECT_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.IS_SWAP_BUTTONS_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LONG_PRESS_DURATION_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.REWIND_ACTION_TYPE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.REWIND_ACTION_TYPE_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.REWIND_DURATION_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SETTINGS_PREFS
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_AMPLITUDE_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_LENGTH_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getAppFilterType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLaunchedCount
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLongPressDuration
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getRewindActionType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getRewindDuration
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getStatusSharedPreferences
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationAmplitude
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationLength
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isHooked
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isSwapButtons
import ru.hepolise.volumekeytrackcontrol.util.StatusSysPropsHelper
import ru.hepolise.volumekeytrackcontrol.util.VibrationType
import ru.hepolise.volumekeytrackcontrol.viewmodel.BootViewModel
import ru.hepolise.volumekeytrackcontrol.viewmodel.BootViewModelFactory
import ru.hepolise.volumekeytrackcontrolmodule.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController?,
    settingsPrefs: SharedPreferences?,
    vibrator: Vibrator?
) {
    val context = LocalContext.current

    val statusPrefs = context.getStatusSharedPreferences()

    val bootViewModel: BootViewModel = viewModel(
        factory = BootViewModelFactory(context.applicationContext)
    )

    val isBootCompleted by bootViewModel.isBootCompleted.collectAsState()
    val isLoading by bootViewModel.isLoading.collectAsState()

    var longPressDuration by remember { mutableIntStateOf(settingsPrefs.getLongPressDuration()) }
    var rewindActionType by remember { mutableStateOf(settingsPrefs.getRewindActionType()) }
    var rewindDuration by remember { mutableIntStateOf(settingsPrefs.getRewindDuration()) }

    var vibrationType by remember { mutableStateOf(settingsPrefs.getVibrationType()) }
    var vibrationLength by remember { mutableIntStateOf(settingsPrefs.getVibrationLength()) }
    var vibrationAmplitude by remember { mutableIntStateOf(settingsPrefs.getVibrationAmplitude()) }
    var isSwapButtons by remember { mutableStateOf(settingsPrefs.isSwapButtons()) }
    var appFilterType by remember { mutableStateOf(settingsPrefs.getAppFilterType()) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }

    val isHooked by produceState(initialValue = false) {
        value = statusPrefs.isHooked().takeIf { settingsPrefs != null } ?: false

        snapshotFlow { isLoading }.collect {
            value = statusPrefs.isHooked().takeIf { settingsPrefs != null } ?: false
        }
    }
    var launchedCount by remember { mutableIntStateOf(statusPrefs.getLaunchedCount()) }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            launchedCount = statusPrefs.getLaunchedCount()
        }
        statusPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            statusPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    settingsPrefs?.also {
        DisposableEffect(Unit) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == REWIND_ACTION_TYPE) {
                    rewindActionType = settingsPrefs.getRewindActionType()
                }
            }
            settingsPrefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                settingsPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (isHooked) {
                        IconButton(onClick = { showResetSettingsDialog = true }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.settings_reset)
                            )
                        }
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Constants.GITHUB_URL.toUri()
                        context.startActivity(intent)
                    }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.about)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(
                icon = if (isHooked) Icons.Default.Done else Icons.Default.Warning,
                title = stringResource(R.string.module_info),
            ) {
                when {
                    context.isInstalledAfterReboot() -> {
                        ModuleStatus(false)
                        ModuleInitError()
                    }

                    isLoading && !isHooked && settingsPrefs != null -> {
                        LoadingAnimation()
                    }

                    else -> {
                        ModuleStatus(isHooked)

                        when {
                            isHooked && !StatusSysPropsHelper.isHooked -> LaunchCounter(
                                launchedCount
                            )

                            settingsPrefs == null -> ModuleIsNotEnabled()
                            isBootCompleted && !isHooked -> ModuleInitError()
                        }
                    }
                }
            }

            if (settingsPrefs != null && isHooked && !context.isInstalledAfterReboot()) {
                SettingsCard(
                    icon = Icons.Default.Settings,
                    title = stringResource(R.string.long_press_settings)
                ) {
                    LongPressSetting(
                        LongPressSettingData(
                            longPressDuration,
                            rewindActionType,
                            rewindDuration
                        ), settingsPrefs
                    ) {
                        longPressDuration = it.longPressDuration
                        rewindActionType = it.rewindActionType
                        rewindDuration = it.rewindDuration
                    }
                    SwapButtonsSetting(
                        isSwapButtons = isSwapButtons,
                        sharedPreferences = settingsPrefs
                    ) {
                        isSwapButtons = it
                    }
                }

                SettingsCard(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.vibration_settings)
                ) {
                    VibrationEffectSetting(
                        value = VibrationSettingData(
                            vibrationType,
                            vibrationLength,
                            vibrationAmplitude
                        ),
                        vibrator = vibrator,
                        sharedPreferences = settingsPrefs
                    ) {
                        vibrationType = it.vibrationType
                        vibrationLength = it.vibrationLength
                        vibrationAmplitude = it.vibrationAmplitude
                    }
                }

                SettingsCard(
                    icon = Icons.Default.Star,
                    title = stringResource(R.string.app_filter),
                    showAction = appFilterType != AppFilterType.DISABLED,
                    onActionClick = { navController?.navigate("appFilter/${appFilterType.key}") }
                ) {
                    AppFilterSetting(
                        value = appFilterType,
                        sharedPreferences = settingsPrefs,
                        onValueChange = { appFilterType = it },
                    )
                }
                if (showResetSettingsDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetSettingsDialog = false },
                        title = { Text(stringResource(R.string.settings_reset)) },
                        text = { Text(stringResource(R.string.settings_reset_message)) },
                        confirmButton = {
                            Button(onClick = {
                                showResetSettingsDialog = false
                                settingsPrefs.edit { clear() }
                                vibrationType = VibrationType.fromKey(EFFECT_DEFAULT_VALUE)
                                vibrationLength = VIBRATION_LENGTH_DEFAULT_VALUE
                                vibrationAmplitude = VIBRATION_AMPLITUDE_DEFAULT_VALUE
                                longPressDuration = LONG_PRESS_DURATION_DEFAULT_VALUE
                                rewindActionType = REWIND_ACTION_TYPE_DEFAULT_VALUE
                                rewindDuration = REWIND_DURATION_DEFAULT_VALUE
                                isSwapButtons = IS_SWAP_BUTTONS_DEFAULT_VALUE
                                appFilterType = AppFilterType.fromKey(
                                    APP_FILTER_TYPE_DEFAULT_VALUE
                                )
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_reset_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Text(stringResource(R.string.yes))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetSettingsDialog = false }) {
                                Text(stringResource(R.string.no))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    showAction: Boolean = false,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                ActionIconButton(
                    imageVector = actionIcon ?: Icons.Default.Edit,
                    contentDescription = null,
                    onClick = { onActionClick?.invoke() },
                    visible = showAction
                )
            }
            content()
        }
    }
}

@Composable
fun ActionIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
    containerSize: Dp = 40.dp,
    visible: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.8f else 1f,
        label = "actionIconScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.6f else 1f,
        label = "actionIconAlpha"
    )
    val color by animateColorAsState(
        targetValue = if (pressed)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        else
            MaterialTheme.colorScheme.onSurface,
        label = "actionIconColor"
    )

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                enabled = visible
            )
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = color,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }
    }
}

@Composable
fun LaunchCounter(launchedCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            stringResource(R.string.module_launch_count),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = launchedCount.toString(),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LoadingAnimation() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
    }
}

@Composable
fun ModuleStatus(isHooked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.module_status),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(
                if (isHooked) R.string.module_status_active
                else R.string.module_status_inactive
            ),
            color = if (isHooked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ModuleIsNotEnabled() {
    Text(
        text = buildAnnotatedString {
            append(stringResource(id = R.string.module_is_not_enabled))
            RecommendedLsposedVersion()
        }
    )
}

@Composable
fun ModuleInitError() {
    Text(
        text = buildAnnotatedString {
            append(stringResource(id = R.string.module_init_error))
            append("\n")
            append("\n")
            withLink(
                LinkAnnotation.Url(
                    url = Constants.GITHUB_NEW_ISSUE_URL,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
            ) {
                append(stringResource(id = R.string.open_an_issue))
            }
            append(" ")
            append(stringResource(id = R.string.if_the_problem_persists))
            RecommendedLsposedVersion()
        }
    )
}

@Composable
private fun Builder.RecommendedLsposedVersion() {
    append("\n")
    append("\n")
    append(stringResource(id = R.string.recommended_lsposed_version))
    append(" ")
    withLink(
        LinkAnnotation.Url(
            url = Constants.LSPOSED_GITHUB_URL,
            styles = TextLinkStyles(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            )
        )
    ) {
        append(stringResource(id = R.string.recommended_lsposed_version_url))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSettingsScreen() {
    SettingsScreen(
        navController = null,
        settingsPrefs = LocalContext.current.getSharedPreferences(
            SETTINGS_PREFS,
            Context.MODE_PRIVATE
        ),
        vibrator = null
    )
}
