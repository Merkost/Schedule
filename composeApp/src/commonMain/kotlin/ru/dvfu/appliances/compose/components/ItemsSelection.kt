package ru.dvfu.appliances.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.dvfu.appliances.model.utils.StringOperation

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> ItemsSelection(
    modifier: Modifier = Modifier,
    radioOptions: List<T>,
    currentOption: State<T?>,
    onSelectedItem: (T) -> Unit,
) where T : StringOperation {

    val (selectedOption, onOptionSelected) = remember {
        mutableStateOf(currentOption.value)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            radioOptions.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .selectable(
                            selected = (option == selectedOption),
                            onClick = {
                                onOptionSelected(option)
                                onSelectedItem(option)
                            }
                        )
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = (option == selectedOption),
                        modifier = Modifier.padding(all = Dp(value = 8F)),
                        onClick = {
                            onOptionSelected(option)
                            onSelectedItem(option)
                        }
                    )
                    Text(
                        text = stringResource(option.stringRes),
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

    }
}
