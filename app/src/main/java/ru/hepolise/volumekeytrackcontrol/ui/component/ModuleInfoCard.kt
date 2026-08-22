package ru.hepolise.volumekeytrackcontrol.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import io.github.libxposed.service.XposedService
import ru.hepolise.volumekeytrackcontrol.R
import ru.hepolise.volumekeytrackcontrol.ui.LocalModuleScope
import ru.hepolise.volumekeytrackcontrol.ui.LocalXposedService
import ru.hepolise.volumekeytrackcontrol.ui.screen.ScopeRequestStatus
import ru.hepolise.volumekeytrackcontrol.util.Constants
import kotlin.time.Duration.Companion.milliseconds

const val MODULE_SCOPE = "system"

@Composable
fun ModuleInfoCard(
    isHooked: Boolean,
    launchedCount: Int,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onScopeRequested: () -> Unit = {}
) {
    val xposedService = LocalXposedService.current
    val moduleScope = LocalModuleScope.current
    val isScopeValid = moduleScope.orEmpty().contains(MODULE_SCOPE)
    val context = LocalContext.current

    var scopeRequestStatus by remember { mutableStateOf<ScopeRequestStatus?>(null) }
    var isScopeRequested by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }

    val scopeCallback = remember {
        object : XposedService.OnScopeEventListener {
            override fun onScopeRequestApproved(approved: List<String>) {
                scopeRequestStatus = ScopeRequestStatus.Success(approved)
                isScopeRequested = true
                showSuccessToast = true
                onScopeRequested()
            }

            override fun onScopeRequestFailed(message: String) {
                scopeRequestStatus = ScopeRequestStatus.Error(message)
                isScopeRequested = false
            }
        }
    }

    val requestScope = {
        if (xposedService != null && !isScopeRequested) {
            scopeRequestStatus = ScopeRequestStatus.Loading
            xposedService.requestScope(listOf(MODULE_SCOPE), scopeCallback)
        }
    }

    LaunchedEffect(showSuccessToast) {
        if (showSuccessToast) {
            snackbarHostState.showSnackbar(
                message = context.getString(
                    R.string.scope_request_success,
                    (scopeRequestStatus as? ScopeRequestStatus.Success)?.approved?.joinToString()
                        ?: ""
                ),
                duration = SnackbarDuration.Short
            )
            kotlinx.coroutines.delay(2000.milliseconds)
            showSuccessToast = false
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isHooked) Icons.Default.Done else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isHooked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.module_info),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.weight(1f))
            }

            ModuleStatus(
                isHooked = isHooked,
                launchedCount = launchedCount
            )

            when {
                xposedService == null -> ModuleIsNotEnabled()

                !isScopeValid && !isScopeRequested -> ScopeStatusSection(
                    status = scopeRequestStatus,
                    onRequestScope = requestScope
                )

                !isHooked -> ModuleInitError()

            }
        }
    }
}

@Composable
fun ModuleStatus(isHooked: Boolean, launchedCount: Int) {
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
    if (isHooked && launchedCount >= 0) {
        LaunchCounter(launchedCount)
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
fun ModuleIsNotEnabled() {
    Text(
        text = buildAnnotatedString {
            append(stringResource(id = R.string.module_is_not_enabled))
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
        }
    )
}

@Composable
private fun ScopeStatusSection(
    status: ScopeRequestStatus?,
    onRequestScope: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.scope_not_selected_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = status,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300)) using
                        SizeTransform(clip = false)
            },
            label = "status_content"
        ) { currentStatus ->
            when (currentStatus) {
                is ScopeRequestStatus.Loading -> {
                    ScopeRequestButton(
                        onRequestScope = onRequestScope,
                        isLoading = true
                    )
                }

                is ScopeRequestStatus.Error -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                R.string.scope_request_error,
                                currentStatus.message
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        ScopeRequestButton(onRequestScope)
                    }
                }

                else -> ScopeRequestButton(onRequestScope)
            }
        }
    }
}

@Composable
private fun ScopeRequestButton(
    onRequestScope: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            isPressed.value = interaction is PressInteraction.Press
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed.value) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isPressed.value) 360f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "icon_rotation"
    )

    Button(
        onClick = onRequestScope,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .indication(interactionSource, ripple()),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.requesting_scope))
        } else {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.request_scope_button))
        }
    }
}