package com.example.composetutorial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.composetutorial.ui.theme.ComposeTutorialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import android.net.Uri
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TextField


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Haetaan asetettu nimi
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val savedName = sharedPref.getString("user_name", "Lexi") ?: "Lexi"
        val file = File(filesDir, "profile_pic.jpg")
        val savedUri = if (file.exists()) Uri.fromFile(file) else null

        setContent {
            ComposeTutorialTheme {
                // Annetaan naville tiedot
                AppNavigation(initialName = savedName, initialUri = savedUri)
            }
        }
    }
    fun saveName(name: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_name", name)
            apply()
        }
    }
}
@Composable
fun AppNavigation(initialName: String, initialUri: Uri?) {
    val navController = rememberNavController()
    val context = LocalContext.current as MainActivity
    var userName by remember { mutableStateOf(initialName) }
    var userImageUri by remember { mutableStateOf(initialUri) }

    NavHost(navController = navController, startDestination = "Keskustelupalsta") {
        //Ensimmäinen tai "pää"näkymä joka on aluksi tehty keskustelupalsta
        composable("Keskustelupalsta") {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    Button(
                        onClick = { navController.navigate("Profiili") },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Edit Profile")
                    }
                    Conversation(userName, userImageUri)
                }
            }
        }
        // Toinen näkymä - Oma profiili
        composable("Profiili") {
            ProfileScreen(
                currentName = userName,
                currentUri = userImageUri,
                onSave = { newName, newUri ->
                    userName = newName
                    userImageUri = newUri

                    // Nimen tallennus
                    context.saveName(newName)

                    navController.navigate("Keskustelupalsta") {
                        popUpTo("Keskustelupalsta") { inclusive = true }
                    }
                }
            )
        }
    }
}
@Composable
fun ProfileScreen(
    currentName: String,
    currentUri: Uri?,
    onSave: (String, Uri?) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var selectedUri by remember { mutableStateOf(currentUri) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = saveImageToInternalStorage(context, uri)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = selectedUri ?: R.drawable.avatar,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Text("Select Your photo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Insert Your Name") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { onSave(nameInput, selectedUri) }) {
                Text("Save and Return")
            }
        }
    }
}
fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "profile_pic.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
data class Message(val author: String, val body: String, val imageResource: Int)
@Composable
fun MessageCard(msg: Message, customUri: Uri?) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        if (customUri != null) {
            AsyncImage(
                model = customUri,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        } else {
            Image(
                painter = painterResource(msg.imageResource),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        var isExpanded by remember { mutableStateOf(false) }
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )

        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                color = surfaceColor,
                modifier = Modifier.animateContentSize().padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
@Composable
fun Conversation(userName: String, userImageUri: Uri?) {
    LazyColumn {
        items(SampleData.conversationSample) { message ->
            if (message.author == "Lexi") {
                MessageCard(message.copy(author = userName), userImageUri)
            } else {
                MessageCard(message, null)
            }
        }
    }
}
@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Preview(showBackground = true)
@Composable
fun PreviewMessageCard() {
    ComposeTutorialTheme {
        Surface {
            MessageCard(
                msg = Message("Lexi", "Hello!", R.drawable.avatar),
                customUri = null
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun PreviewConversation() {
    ComposeTutorialTheme {
        Conversation(userName = "Lexi", userImageUri = null)
    }
}
object SampleData {
    // Sample conversation data
    val conversationSample = listOf(
        Message(
            "Lexi",
            "Test...Test...Test...",
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            """List of Android versions:
            |Android KitKat (API 19)
            |Android Lollipop (API 21)
            |Android Marshmallow (API 23)
            |Android Nougat (API 24)
            |Android Oreo (API 26)
            |Android Pie (API 28)
            |Android 10 (API 29)
            |Android 11 (API 30)
            |Android 12 (API 31)""".trim(),
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            """I think Kotlin is my favorite programming language.
            |It's so much fun!""".trim(),
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            "Searching for alternatives to XML layouts...",
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            """Hey, take a look at Jetpack Compose, it's great!
            |It's the Android's modern toolkit for building native UI.
            |It simplifies and accelerates UI development on Android.
            |Less code, powerful tools, and intuitive Kotlin APIs :)""".trim(),
            R.drawable.avatar
        ),
        Message(
            "Pentti",
            "Im enjoying my retirement at peace thank you and wont buy anything that you are selling",
            R.drawable.pentti
        ),
        Message(
            "Lexi",
            "It's available from API 21+ :)",
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            "Writing Kotlin for UI seems so natural, Compose where have you been all my life?",
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            "Android Studio next version's name is Arctic Fox",
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            "Android Studio Arctic Fox tooling for Compose is top notch ^_^",
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            "I didn't know you can now run the emulator directly from Android Studio",
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            "Compose Previews are great to check quickly how a composable layout looks like",
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            "Previews are also interactive after enabling the experimental setting",
            R.drawable.avatar
        ),
        Message(
            "Lexi",
            "Have you tried writing build.gradle with KTS?",
            R.drawable.avatar
        ),

    )
}