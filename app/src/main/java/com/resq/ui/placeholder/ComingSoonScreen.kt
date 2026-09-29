package com.resq.ui.placeholder

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.ResQDimens

@Composable
fun ComingSoonScreen(title: String, milestone: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = ResQDimens.page)) {
        ResQHeader(title)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Construction, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(14.dp))
            Text("Planned for $milestone", fontWeight = FontWeight.ExtraBold)
            Text("This destination is wired and ready for its real data source.")
        }
    }
}
