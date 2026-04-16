package ru.dvfu.appliances.compose.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.androidx.compose.koinViewModel
import ru.dvfu.appliances.R
import ru.dvfu.appliances.compose.*
import ru.dvfu.appliances.compose.components.FullscreenLoading
import ru.dvfu.appliances.compose.viewmodels.AppliancesViewModel
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.isAdmin
import ru.dvfu.appliances.ui.ViewState

@ExperimentalAnimationApi
@InternalCoroutinesApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Appliances(navController: NavController, backPress: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: AppliancesViewModel = koinViewModel()
    val appliancesState by viewModel.appliancesState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            ScheduleAppBar(
                title = stringResource(R.string.appliances),
                backClick = backPress,
                actionAdd = currentUser.isAdmin,
                addClick = { navController.navigate(MainDestinations.NEW_APPLIANCE_ROUTE) },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = appliancesState) {
                is ViewState.Error -> EmptyStateBox(
                    icon = Icons.Outlined.DevicesOther,
                    message = stringResource(R.string.error_occured),
                )
                is ViewState.Loading -> FullscreenLoading()
                is ViewState.Success -> {
                    val active = state.data[true].orEmpty()
                    val inactive = state.data[false].orEmpty()
                    AppliancesGrid(
                        active = active,
                        inactive = inactive,
                        onApplianceClick = { appliance ->
                            navController.navigate(
                                MainDestinations.APPLIANCE_ROUTE,
                                Arguments.APPLIANCE to appliance,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppliancesGrid(
    active: List<Appliance>,
    inactive: List<Appliance>,
    onApplianceClick: (Appliance) -> Unit,
) {
    var showInactive by rememberSaveable { mutableStateOf(false) }

    if (active.isEmpty() && inactive.isEmpty()) {
        EmptyStateBox(icon = Icons.Outlined.DevicesOther, message = stringResource(R.string.appliances))
        return
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(active) { appliance ->
            ItemAppliance(appliance = appliance, applianceClicked = onApplianceClick)
        }
        if (inactive.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                InactiveSectionHeader(
                    count = inactive.size,
                    expanded = showInactive,
                    onToggle = { showInactive = !showInactive },
                )
            }
            if (showInactive) {
                items(inactive) { appliance ->
                    ItemAppliance(appliance = appliance, applianceClicked = onApplianceClick)
                }
            }
        }
    }
}

@Composable
private fun InactiveSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "inactiveChevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Неактивные ($count)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
fun ItemAppliance(appliance: Appliance, applianceClicked: (Appliance) -> Unit) {
    Card(
        onClick = { applianceClicked(appliance) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appliance.active) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            ApplianceImage(appliance, modifier = Modifier.size(64.dp))
            ApplianceName(appliance)
            if (!appliance.active) {
                Text(
                    text = "Неактивен",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun EmptyStateBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ApplianceName(appliance: Appliance, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    Text(
        modifier = modifier,
        text = appliance.name,
        maxLines = 2,
        textAlign = textAlign,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
fun ApplianceImage(appliance: Appliance, modifier: Modifier = Modifier) {
    val applianceColor = Color(appliance.color)
    val hasUsableColor = applianceColor.alpha >= 0.1f && applianceColor != Color.White
    val fallback = MaterialTheme.colorScheme.primary
    val tint = if (hasUsableColor) applianceColor else fallback
    val textColor = if (hasUsableColor) {
        if (tint.luminance() > 0.6f) MaterialTheme.colorScheme.onSurface else tint
    } else fallback

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .aspectRatio(1f)
            .background(tint.copy(alpha = if (hasUsableColor) 0.18f else 0.12f))
            .border(1.5.dp, tint.copy(alpha = 0.45f), CircleShape),
    ) {
        Text(
            text = if (appliance.name.isEmpty()) "?" else appliance.name.first().uppercase(),
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            color = textColor,
        )
    }
}
