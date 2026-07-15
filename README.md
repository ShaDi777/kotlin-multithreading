# Kotlin Multithreading

Репозиторий содержит набор практических заданий по многопоточному программированию на Kotlin.
Каждое задание реализует конкретный алгоритм или структуру данных для конкурентного выполнения.

## Структура проекта

Проект организован в виде нумерованных директорий, каждая из которых содержит отдельное задание:

### Базовые задания

| № | Задание | Описание |
|---|---------|----------|
| [01](01-possible-executions-analysis/) | Анализ возможных исполнений | Анализ всех возможных исполнений программы в модели чередования операций |
| [02](02-michael-scott-queue/) | Michael-Scott Queue | Реализация классической lock-free очереди |
| [03](03-treiber-stack-with-elimination/) | Stack with Elimination | Стек Трейбера с оптимизацией через технику elimination |
| [04](04-lamport-lock-fail/) | Ошибочный алгоритм Лампорта | Анализ некорректной реализации алгоритма Лампорта |
| [05](05-fine-grained-bank/) | Синхронизация с помощью тонкой блокировки | Реализация банка с fine-grained блокировками |
| [06](06-monotonic-clock/) | Монотонные часы | Алгоритм монотонных часов на регулярных регистрах |
| [07](07-faa-queue/) | FAA-based Queue | Очередь на основе примитива Fetch-and-Add |
| [08](08-flat-combining-queue/) | Flat Combining Queue | Очередь с использованием техники flat combining |
| [09](09-read-write-bank/) | Read-Write Bank | Банк с read-write блокировками |
| [10](10-mcs-lock/) | MCS Lock | Справедливая блокировка MCS с park/unpark |

### Дополнительные задания (автор: ShaDi777)

| № | Задание | Описание |
|---|---------|----------|
| [11](11-casn-ShaDi777/) | Multi-Word CAS | Атомарная операция cas2 для AtomicArray |
| [12](12-lock-free-bank-ShaDi777/) | Lock-Free Bank | Безблокировочная реализация банка |
| [13](13-removals-from-the-middle-ShaDi777/) | Constant-time Removals | Michael-Scott queue с удалением за O(1) |
| [14](14-stm-bank-ShaDi777/) | STM Bank | Банк на основе Software Transactional Memory |
| [15](15-hash-table-ShaDi777/) | Lock-Free Hash Table | Lock-free hash таблица с открытой адресацией |
| [16](16-dynamic-array-ShaDi777/) | Dynamic Array | Lock-free динамический массив |

## Требования

* JDK 11 или выше
* Gradle (встроен в каждый подпроект)

## Сборка и тестирование

Для сборки и тестирования конкретного задания перейдите в соответствующую директорию и выполните:

```bash
cd <номер-задания>-<название>
./gradlew test        # Linux/macOS
gradlew test          # Windows
./gradlew build       # Полная сборка
```

## Тематические группы заданий

### Очереди (Queues)
* [Michael-Scott Queue](02-michael-scott-queue/)
* [FAA-based Queue](07-faa-queue/)
* [Flat Combining Queue](08-flat-combining-queue/)
* [MS Queue с constant-time removal](13-removals-from-the-middle-ShaDi777/)

### Стеки (Stacks)
* [Treiber Stack with Elimination](03-treiber-stack-with-elimination/)

### Блокировки (Locks)
* [MCS Lock](10-mcs-lock/)
* [Анализ алгоритма Лампорта](04-lamport-lock-fail/)

### Банковские задачи (Bank)
* [Fine-Grained Bank](05-fine-grained-bank/)
* [Read-Write Bank](09-read-write-bank/)
* [Lock-Free Bank](12-lock-free-bank-ShaDi777/)
* [STM Bank](14-stm-bank-ShaDi777/)

### Хэш-таблицы и массивы
* [Lock-Free Hash Table](15-hash-table-ShaDi777/)
* [Dynamic Array](16-dynamic-array-ShaDi777/)

### Прочее
* [Multi-Word CAS](11-casn-ShaDi777/)
* [Monotonic Clock](06-monotonic-clock/)
* [Анализ исполнений](01-possible-executions-analysis/)

## Формат сдачи заданий

Большинство заданий требуют:
1. Реализации алгоритма в указанном файле (обычно `src/Solution.kt` или аналогичный)
2. Указания вашего имени в строке `@author` в заголовке файла
3. Прохождения всех тестов через `./gradlew test`

Некоторые задания (например, [01](01-possible-executions-analysis/), [04](04-lamport-lock-fail/)) требуют написания решения в файле `solution.txt`.

## Рекомендации

* Внимательно читайте README в каждой папке задания
* Изучайте предоставленные шаблоны кода в директориях `src/`
* Запускайте тесты после каждой значимой изменения
* Для отладки используйте `./gradlew build --info`

## Дополнительные материалы

В описаниях заданий приведены ссылки на научные статьи и оригинальные работы:
* [The Art of Multiprocessor Programming](https://www.elsevier.com/books/the-art-of-multiprocessor-programming/herlihy/978-0-12-397337-5) by Herlihy & Shavit
* [Fast Concurrent Queues for x86 Processors](https://www.cs.tau.ac.il/~mad/publications/ppopp2013-x86queues.pdf)
* [A Practical Multi-Word Compare-and-Swap Operation](https://www.cl.cam.ac.uk/~tjh243/publications/disc01.pdf) by Harris et al.
* [Concurrent Reading and Writing of Clocks](http://lamport.azurewebsites.net/pubs/lamport-concurrent-clocks.pdf) by Lamport
