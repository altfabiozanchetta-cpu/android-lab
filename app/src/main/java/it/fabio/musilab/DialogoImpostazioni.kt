package it.fabio.musilab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Menu Impostazioni: suono delle note e salvataggio all'uscita. */
@Composable
fun DialogoImpostazioni(config: StatoConfig, onChiudi: () -> Unit) {
    AlertDialog(
        onDismissRequest = onChiudi,
        confirmButton = { TextButton(onClick = onChiudi) { Text("Chiudi") } },
        title = { Text("Impostazioni") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Suono nella modalità scale", fontWeight = FontWeight.Bold)
                for (s in Suono.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { config.suono = s },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = config.suono == s, onClick = { config.suono = s })
                        Text(s.nome)
                    }
                }
                Text(
                    "Nel metronomo si sente sempre il click.",
                    fontSize = 12.sp,
                    color = Color(0xFF9FB0CC),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { config.salvaAllUscita = !config.salvaAllUscita },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Salva le impostazioni all'uscita", modifier = Modifier.weight(1f))
                    Switch(checked = config.salvaAllUscita, onCheckedChange = { config.salvaAllUscita = it })
                }
                Text(
                    "Tempo, tonalità, scala, strumento e vista si ritrovano alla riapertura.",
                    fontSize = 12.sp,
                    color = Color(0xFF9FB0CC)
                )
            }
        }
    )
}
