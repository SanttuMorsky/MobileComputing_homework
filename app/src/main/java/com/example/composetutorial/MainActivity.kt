package com.example.composetutorial

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.composetutorial.ui.theme.ComposeTutorialTheme
import java.io.File
import java.io.FileOutputStream


class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val CHANNEL_ID = "upside_down_channel"
    private var isAppInForeground = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initializing the accelerometer used with sending notifications
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val savedName = sharedPref.getString("user_name", "Lexi") ?: "Lexi"
        val file = File(filesDir, "profile_pic.jpg")
        val savedUri = if (file.exists()) Uri.fromFile(file) else null

        setContent {
            ComposeTutorialTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted -> /* Handle permission result */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                AppNavigation(initialName = savedName, initialUri = savedUri)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Movement Alerts"
            val descriptionText = "Notification by rotating phone upside down"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.avatar)
            .setContentTitle("You got mail!")
            .setContentText("Press here to read the new message")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val yAxis = event.values[1]
            // Triggeröi kun puhelimen kääntää "ylösalasin"
            if (yAxis < -7.0) {
                sendNotification()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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