from collections import deque


def critical_path(tasks):
    successors = {task: [] for task in tasks}
    degree = {task: 0 for task in tasks}

    for task, (duration, predecessors) in tasks.items():
        if duration <= 0:
            raise ValueError("Длительность должна быть положительной")
        for predecessor in predecessors:
            if predecessor not in tasks:
                raise ValueError(f"Неизвестная задача: {predecessor}")
            successors[predecessor].append(task)
            degree[task] += 1

    queue = deque(task for task in tasks if degree[task] == 0)
    order = []

    while queue:
        task = queue.popleft()
        order.append(task)
        for next_task in successors[task]:
            degree[next_task] -= 1
            if degree[next_task] == 0:
                queue.append(next_task)

    if len(order) != len(tasks):
        raise ValueError("Обнаружен цикл зависимостей")

    es, ef = {}, {}
    for task in order:
        duration, predecessors = tasks[task]
        es[task] = max((ef[p] for p in predecessors), default=0)
        ef[task] = es[task] + duration

    total = max(ef.values(), default=0)
    ls, lf = {}, {}

    for task in reversed(order):
        duration = tasks[task][0]
        lf[task] = min((ls[n] for n in successors[task]), default=total)
        ls[task] = lf[task] - duration

    slack = {task: ls[task] - es[task] for task in tasks}
    path = []
    current = next((task for task in order
                    if slack[task] == 0 and es[task] == 0), None)

    while current is not None:
        path.append(current)
        current = next((n for n in successors[current]
                        if slack[n] == 0 and es[n] == ef[current]), None)

    return order, es, ef, ls, lf, slack, path, total


def main():
    tasks = {
        "Техническое задание": (5, []),
        "Архитектура клиента": (6, ["Техническое задание"]),
        "Арт-bible и стиль": (7, ["Техническое задание"]),
        "Ядро игрового цикла": (20, ["Архитектура клиента"]),
        "Мета-прогрессия": (15, ["Ядро игрового цикла"]),
        "Экономика и монетизация": (12, ["Ядро игрового цикла"]),
        "Персонажи и анимации": (25, ["Арт-bible и стиль"]),
        "Интерфейсы (UI/UX)": (14, ["Арт-bible и стиль", "Архитектура клиента"]),
        "Серверная часть": (18, ["Архитектура клиента"]),
        "Аналитика и SDK": (8, ["Ядро игрового цикла"]),
        "Звук и музыка": (10, ["Арт-bible и стиль"]),
        "Интеграция контента": (12, [
            "Персонажи и анимации", "Интерфейсы (UI/UX)", "Мета-прогрессия"
        ]),
        "Альфа-сборка": (3, [
            "Интеграция контента", "Серверная часть",
            "Экономика и монетизация", "Аналитика и SDK", "Звук и музыка"
        ]),
        "Внутреннее тестирование": (10, ["Альфа-сборка"]),
        "Бета-сборка": (4, ["Внутреннее тестирование"])
    }

    order, es, ef, ls, lf, slack, path, total = critical_path(tasks)
    print("Длительность проекта:", total, "раб. дней")
    print("Критический путь:", " -> ".join(path))
    print(f"{'Задача':<28}{'ES':>4}{'EF':>5}{'LS':>5}{'LF':>5}{'Резерв':>8}")

    for task in order:
        print(f"{task:<28}{es[task]:>4}{ef[task]:>5}"
              f"{ls[task]:>5}{lf[task]:>5}{slack[task]:>8}")


if __name__ == "__main__":
    main()
