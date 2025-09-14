package com.example.todolist2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todolist2.ui.theme.TodoList2Theme

class MainActivity : ComponentActivity() {
    
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val recognizedText = results[0]
                // 음성 인식 결과를 처리하는 로직은 AddTodoDialog에서 처리
                onSpeechResult?.invoke(recognizedText)
            }
        }
    }
    
    private var onSpeechResult: ((String) -> Unit)? = null
    
    fun startSpeechRecognition(onResult: (String) -> Unit) {
        onSpeechResult = onResult
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 요청
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR") // 한국어 설정
            putExtra(RecognizerIntent.EXTRA_PROMPT, "할 일을 말씀해주세요")
        }
        
        speechRecognizerLauncher.launch(intent)
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한이 승인되면 음성 인식 시작
            onSpeechResult?.let { startSpeechRecognition(it) }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoList2Theme {
                TodoApp(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(mainActivity: MainActivity) {
    var todoItems by remember { mutableStateOf(listOf<TodoItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var isDragMode by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableStateOf(-1) }
    var isLoading by remember { mutableStateOf(true) }
    
    val listState = rememberLazyListState()
    
    // 로딩 화면 표시
    LaunchedEffect(Unit) {
        delay(2000) // 2초 로딩
        isLoading = false
    }
    
    if (isLoading) {
        LoadingScreen()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "할 일 목록",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (todoItems.isNotEmpty()) {
                            val completedCount = todoItems.count { it.isCompleted }
                            val totalCount = todoItems.size
                            Text(
                                text = "$completedCount/$totalCount 완료",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (todoItems.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "전체 삭제",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "할 일 추가"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 드래그 모드 안내 메시지
            if (isDragMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "화살표 버튼으로 순서를 변경하세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                isDragMode = false
                                draggedIndex = -1
                            }
                        ) {
                            Text("완료")
                        }
                    }
                }
            }
            
            if (todoItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "할 일을 추가해보세요!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "오른쪽 아래 + 버튼을 눌러서\n새로운 할 일을 추가할 수 있습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(todoItems) { index, item ->
                        TodoItemCard(
                            item = item,
                            index = index,
                            isDragMode = isDragMode,
                            isDragged = draggedIndex == index,
                            todoItems = todoItems,
                            onToggleComplete = { 
                                // 현재 화면 위치(인덱스) 기반으로 완료 상태 토글
                                todoItems = todoItems.mapIndexed { i, todo ->
                                    if (i == index) todo.copy(isCompleted = !todo.isCompleted)
                                    else todo
                                }
                            },
                            onDelete = {
                                // 현재 화면 위치(인덱스) 기반으로 삭제
                                todoItems = todoItems.filterIndexed { i, _ -> i != index }
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val newList = todoItems.toMutableList()
                                    val item = newList.removeAt(index)
                                    newList.add(index - 1, item)
                                    todoItems = newList
                                    // 순서 변경 후 드래그 모드 완전 리셋
                                    isDragMode = false
                                    draggedIndex = -1
                                }
                            },
                            onMoveDown = {
                                if (index < todoItems.size - 1) {
                                    val newList = todoItems.toMutableList()
                                    val item = newList.removeAt(index)
                                    newList.add(index + 1, item)
                                    todoItems = newList
                                    // 순서 변경 후 드래그 모드 완전 리셋
                                    isDragMode = false
                                    draggedIndex = -1
                                }
                            },
                            onLongPress = {
                                isDragMode = true
                                draggedIndex = index
                            },
                            onDrag = { },
                            onDragEnd = { }
                        )
                    }
                }
            }
        }
    }

    // 할 일 추가 다이얼로그
    if (showAddDialog) {
        AddTodoDialog(
            mainActivity = mainActivity,
            onDismiss = { showAddDialog = false },
            onAdd = { text ->
                if (text.isNotBlank()) {
                    todoItems = todoItems + TodoItem(text = text.trim())
                }
                showAddDialog = false
            }
        )
    }

    // 전체 삭제 확인 다이얼로그
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("전체 삭제") },
            text = { Text("모든 할 일을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        todoItems = emptyList()
                        showClearDialog = false
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun TodoItemCard(
    item: TodoItem,
    index: Int,
    isDragMode: Boolean,
    isDragged: Boolean,
    todoItems: List<TodoItem>,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onLongPress: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { 
                            if (!isDragMode) {
                                onToggleComplete() 
                            }
                        },
                        onLongPress = { 
                            onLongPress() 
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDragged) 8.dp else 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isDragged) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 완료 상태 표시 원
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isCompleted) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isCompleted) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 할 일 텍스트
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.isCompleted) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isCompleted) 
                        TextDecoration.LineThrough 
                    else 
                        TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
                
                // 액션 버튼들
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 삭제 버튼 (드래그 모드가 아닐 때만 표시)
                    if (!isDragMode) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "삭제",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // 순서 변경 버튼들 (드래그 모드일 때만 표시)
                    if (isDragMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 위로 이동 버튼
                            if (index > 0) {
                                IconButton(
                                    onClick = onMoveUp,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "위로 이동",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            // 아래로 이동 버튼
                            if (index < todoItems.size - 1) {
                                IconButton(
                                    onClick = onMoveDown,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "아래로 이동",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTodoDialog(
    mainActivity: MainActivity,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "새 할 일 추가",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "새로운 할 일을 입력해주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // 텍스트 입력 필드와 음성 인식 버튼
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("할 일을 입력하세요") },
                        placeholder = { Text("예: 쇼핑하기, 운동하기...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    // 음성 인식 버튼
                    IconButton(
                        onClick = {
                            isListening = true
                            mainActivity.startSpeechRecognition { recognizedText ->
                                text = recognizedText
                                isListening = false
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isListening) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    ) {
                        Text(
                            text = if (isListening) "🎤" else "🎙️",
                            fontSize = 20.sp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // 음성 인식 상태 표시
                if (isListening) {
                    Spacer(modifier = Modifier.height(8.dp))
    Text(
                        text = "🎤 음성을 듣고 있습니다...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(text) },
                enabled = text.isNotBlank(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun LoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 로딩 아이콘 (체크리스트)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 체크리스트 아이콘
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 체크리스트 항목들
                        repeat(3) { index ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 체크박스
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            if (index < 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(2.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (index < 2) {
                                        Text(
                                            text = "✓",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                // 텍스트 라인
                                Box(
                                    modifier = Modifier
                                        .size(16.dp, 2.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onPrimary,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
            
            // 로딩 텍스트
            Text(
                text = "할 일 목록",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "로딩 중...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 로딩 인디케이터
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TodoAppPreview() {
    TodoList2Theme {
        // Preview에서는 MainActivity를 null로 전달
        // 실제 앱에서는 정상적으로 작동함
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    TodoList2Theme {
        LoadingScreen()
    }
}