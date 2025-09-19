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
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
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
    
    fun startSpeechRecognition(prompt: String, onResult: (String) -> Unit) {
        onSpeechResult = onResult
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 요청
            pendingPrompt = prompt
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR") // 한국어 설정
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        
        speechRecognizerLauncher.launch(intent)
    }
    
    private var pendingPrompt: String? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한이 승인되면 음성 인식 시작
            val prompt = pendingPrompt ?: "할 일을 말씀해주세요"
            onSpeechResult?.let { startSpeechRecognition(prompt, it) }
            pendingPrompt = null
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

enum class PageType {
    TODO, SHOPPING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(mainActivity: MainActivity) {
    val todoRepository = remember { TodoRepository(mainActivity) }
    var currentPage by remember { mutableStateOf(PageType.TODO) }
    var todoItems by remember { mutableStateOf(listOf<TodoItem>()) }
    var shoppingItems by remember { mutableStateOf(listOf<TodoItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var draggedItemId by remember { mutableStateOf<Long?>(null) }
    var draggedIndex by remember { mutableStateOf(-1) }
    var dragPointerOffset by remember { mutableStateOf(0f) }
    var cumulativeItemOffset by remember { mutableStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<Long, Int>() }
    var isLoading by remember { mutableStateOf(true) }
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val defaultItemHeightPx = with(LocalDensity.current) { 72.dp.toPx() }
    val isDragging = draggedItemId != null
    val currentItems = if (currentPage == PageType.TODO) todoItems else shoppingItems

    
    // 앱 시작 시 저장된 데이터 로드
    LaunchedEffect(Unit) {
        delay(1000) // 1초 로딩
        todoItems = todoRepository.loadTodos()
        shoppingItems = todoRepository.loadShoppingItems()
        isLoading = false
    }
    
    // todoItems 변경 시 자동 저장
    LaunchedEffect(todoItems) {
        if (!isLoading) { // 로딩 중이 아닐 때만 저장
            todoRepository.saveTodos(todoItems)
        }
    }
    
    // shoppingItems 변경 시 자동 저장
    LaunchedEffect(shoppingItems) {
        if (!isLoading) { // 로딩 중이 아닐 때만 저장
            todoRepository.saveShoppingItems(shoppingItems)
        }
    }
    
    // 앱 종료 시 데이터 저장 보장
    DisposableEffect(Unit) {
        onDispose {
            todoRepository.saveTodos(todoItems)
            todoRepository.saveShoppingItems(shoppingItems)
        }
    }
    
    if (isLoading) {
        LoadingScreen()
        return
    }

    Scaffold(
        topBar = {
            val currentItems = if (currentPage == PageType.TODO) todoItems else shoppingItems
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = if (currentPage == PageType.TODO) "할 일 목록" else "살 것 목록",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (currentItems.isNotEmpty()) {
                            val completedCount = currentItems.count { it.isCompleted }
                            val totalCount = currentItems.size
                            Text(
                                text = "$completedCount/$totalCount 완료",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            currentPage = if (currentPage == PageType.TODO) 
                                PageType.SHOPPING else PageType.TODO 
                        }
                    ) {
                        Icon(
                            imageVector = if (currentPage == PageType.TODO) 
                                Icons.Default.ShoppingCart else Icons.Default.List,
                            contentDescription = if (currentPage == PageType.TODO) 
                                "살 것 목록으로 전환" else "할 일 목록으로 전환",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    if (currentItems.isNotEmpty()) {
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
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 음성 인식 버튼
                FloatingActionButton(
                    onClick = {
                        isListening = true
                        val promptText = if (currentPage == PageType.TODO) "할 일을 말씨해주세요" else "살 것을 말씨해주세요"
                        mainActivity.startSpeechRecognition(promptText) { text ->
                            recognizedText = text
                            isListening = false
                            showAddDialog = true
                        }
                    },
                    containerColor = if (isListening) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        text = if (isListening) "🎤" else "🎙️",
                        fontSize = 20.sp
                    )
                }
                
                // 아이템 추가 버튼
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (currentPage == PageType.TODO) "할 일 추가" else "살 것 추가"
                    )
                }
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

            if (isDragging) {
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
                            text = "위나 아래로 드래그하면 순서가 바뀌어요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            if (currentItems.isEmpty()) {
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
                            text = if (currentPage == PageType.TODO) "할 일을 추가해보세요!" else "살 것을 추가해보세요!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (currentPage == PageType.TODO) 
                                "오른쪽 아래 + 버튼을 눌러서\n새로운 할 일을 추가할 수 있습니다" else
                                "오른쪽 아래 + 버튼을 눌러서\n새로운 살 것을 추가할 수 있습니다",
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

                    itemsIndexed(
                        items = currentItems,
                        key = { _, todo -> todo.id }
                    ) { index, item ->
                        val translation = if (draggedItemId == item.id) {
                            dragPointerOffset - cumulativeItemOffset
                        } else {
                            0f
                        }

                        TodoItemCard(
                            item = item,
                            isDragged = draggedItemId == item.id,
                            dragTranslation = translation,
                            isAnyDragging = isDragging,
                            onToggleComplete = {
                                if (currentPage == PageType.TODO) {
                                    todoItems = todoItems.map { todo ->
                                        if (todo.id == item.id) todo.copy(isCompleted = !todo.isCompleted) else todo
                                    }
                                } else {
                                    shoppingItems = shoppingItems.map { todo ->
                                        if (todo.id == item.id) todo.copy(isCompleted = !todo.isCompleted) else todo
                                    }
                                }
                            },
                            onDelete = {
                                if (currentPage == PageType.TODO) {
                                    todoItems = todoItems.filterNot { it.id == item.id }
                                } else {
                                    shoppingItems = shoppingItems.filterNot { it.id == item.id }
                                }
                            },
                            onDragStart = {
                                draggedItemId = item.id
                                draggedIndex = index
                                dragPointerOffset = 0f
                                cumulativeItemOffset = 0f
                            },
                            onDrag = { deltaY ->
                                dragPointerOffset += deltaY
                                var translationDelta = dragPointerOffset - cumulativeItemOffset

                                while (draggedIndex > 0) {
                                    val previousItem = currentItems.getOrNull(draggedIndex - 1) ?: break
                                    val threshold = itemHeights[previousItem.id]?.toFloat() ?: defaultItemHeightPx
                                    if (translationDelta < -threshold / 2f) {
                                        val updated = currentItems.toMutableList()
                                        val moving = updated.removeAt(draggedIndex)
                                        updated.add(draggedIndex - 1, moving)
                                        if (currentPage == PageType.TODO) {
                                            todoItems = updated
                                        } else {
                                            shoppingItems = updated
                                        }
                                        draggedIndex -= 1
                                        cumulativeItemOffset -= threshold
                                        translationDelta = dragPointerOffset - cumulativeItemOffset
                                    } else {
                                        break
                                    }
                                }

                                while (draggedIndex < currentItems.lastIndex) {
                                    val nextItem = currentItems.getOrNull(draggedIndex + 1) ?: break
                                    val threshold = itemHeights[nextItem.id]?.toFloat() ?: defaultItemHeightPx
                                    if (translationDelta > threshold / 2f) {
                                        val updated = currentItems.toMutableList()
                                        val moving = updated.removeAt(draggedIndex)
                                        updated.add(draggedIndex + 1, moving)
                                        if (currentPage == PageType.TODO) {
                                            todoItems = updated
                                        } else {
                                            shoppingItems = updated
                                        }
                                        draggedIndex += 1
                                        cumulativeItemOffset += threshold
                                        translationDelta = dragPointerOffset - cumulativeItemOffset
                                    } else {
                                        break
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedItemId = null
                                draggedIndex = -1
                                dragPointerOffset = 0f
                                cumulativeItemOffset = 0f
                            },
                            onMeasured = { height ->
                                val current = itemHeights[item.id]
                                if (current != height) {
                                    itemHeights[item.id] = height
                                }
                            }
                        )
                    }

                }
            }
        }
    }

    // 할 일 추가 다이얼로그
    if (showAddDialog) {
        AddTodoDialog(
            initialText = recognizedText,
            onDismiss = { 
                showAddDialog = false
                recognizedText = "" // 다이얼로그 닫을 때 음성 인식 텍스트 리셋
            },
            onAdd = { text ->
                if (text.isNotBlank()) {
                    val newItem = TodoItem(
                        text = text.trim(), 
                        type = if (currentPage == PageType.TODO) ItemType.TODO else ItemType.SHOPPING
                    )
                    if (currentPage == PageType.TODO) {
                        todoItems = todoItems + newItem
                    } else {
                        shoppingItems = shoppingItems + newItem
                    }
                }
                showAddDialog = false
                recognizedText = "" // 추가 후 음성 인식 텍스트 리셋
            },
            currentPageType = currentPage
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
                        if (currentPage == PageType.TODO) {
                            todoItems = emptyList()
                            todoRepository.clearTodos()
                        } else {
                            shoppingItems = emptyList()
                            todoRepository.clearShoppingItems()
                        }
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
    isDragged: Boolean,
    dragTranslation: Float,
    isAnyDragging: Boolean,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMeasured: (Int) -> Unit
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
        val scale by animateFloatAsState(
            targetValue = if (isDragged) 1.03f else 1f,
            animationSpec = tween(150),
            label = "dragScale"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = if (isDragged) dragTranslation else 0f
                    scaleX = scale
                    scaleY = scale
                }
                .zIndex(if (isDragged) 1f else 0f)
                .pointerInput(item.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            onDragStart()
                        },
                         onDrag = { _, dragAmount ->
                             onDrag(dragAmount.y)
                         },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragEnd()
                        }
                    )
                }
                .pointerInput(item.id) {
                    detectTapGestures(
                        onTap = {
                            if (!isAnyDragging) {
                                onToggleComplete()
                            }
                        }
                    )
                }
                .onGloballyPositioned { coordinates ->
                    onMeasured(coordinates.size.height)
                },
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDragged) 12.dp else 2.dp
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

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDelete,
                        enabled = !isAnyDragging,
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
            }
        }
    }
}

@Composable
fun AddTodoDialog(
    initialText: String = "",
    currentPageType: PageType = PageType.TODO,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    
    val isTodoPage = currentPageType == PageType.TODO
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (isTodoPage) "새 할 일 추가" else "새 살 것 추가",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = if (isTodoPage) "새로운 할 일을 입력해주세요" else "새로 살 것을 입력해주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (isTodoPage) "할 일을 입력하세요" else "살 것을 입력하세요") },
                    placeholder = { 
                        Text(if (isTodoPage) "예: 쇼핑하기, 운동하기..." else "예: 우유, 빵, 계란...")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
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