import heapq
import random


def top_k(scores, k):
    if k <= 0:
        return []

    heap = []
    for player, score in scores:
        entry = (score, -player)
        if len(heap) < k:
            heapq.heappush(heap, entry)
        elif entry > heap[0]:
            heapq.heapreplace(heap, entry)

    return [(-player, score) for score, player in sorted(heap, reverse=True)]


def main():
    rng = random.Random(5)
    scores = [(player, rng.randint(0, 1000)) for player in range(1, 9)]
    print("Результаты:", scores)
    print("Топ-3:", top_k(scores, 3))

    scores = [(player, rng.randint(0, 10**6)) for player in range(100000)]
    result = top_k(scores, 5)
    print("Топ-5 среди 100 000 игроков:")

    for place, (player, score) in enumerate(result, 1):
        print(f"{place}. Игрок {player}, очки {score}")


if __name__ == "__main__":
    main()
