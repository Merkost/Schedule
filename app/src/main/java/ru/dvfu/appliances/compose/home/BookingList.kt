package ru.dvfu.appliances.compose.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import org.koin.androidx.compose.getViewModel
import ru.dvfu.appliances.compose.*
import ru.dvfu.appliances.compose.appliance.LoadingItem
import ru.dvfu.appliances.model.repository.entity.Booking
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.ui.ViewState
import java.time.LocalDateTime

@OptIn(
    ExperimentalCoilApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.material.ExperimentalMaterialApi::class
)
@Composable
fun BookingList(navController: NavController) {
    val viewModel: BookingListViewModel = getViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val list by viewModel.bookingList.collectAsState()

    Scaffold(topBar = {
        ScheduleAppBar(
            title = "Бронирования",
            backClick = { navController.popBackStack() })
    }) {
        Crossfade(targetState = uiState) {
            when(it) {
                is ViewState.Error -> {
                    //ErrorView()
                }
                is ViewState.Loading -> {
                    LoadingItem(Modifier.fillMaxSize())
                }
                is ViewState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(list) { book ->
                            Card(modifier = Modifier.padding(4.dp)) {
                                Column {
                                    ItemUser(user = book.user, userClicked = {
                                        navController.navigate(MainDestinations.USER_DETAILS_ROUTE, Arguments.USER to book.user)
                                    })
                                    book.appliance?.let { ItemAppliance(appliance = it, applianceClicked = {}) }

                                }
                            }
                        }
                    }
                }
            }
        }

    }
}