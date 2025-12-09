#!/usr/bin/env python3
import sys
import os
import time
from pathlib import Path

def test_mode():
    """Тестовый режим"""
    print("Python agent DocStyler started!")
    print("Version: 2.0.0")
    print("MESSAGE:Python agent работает корректно")
    print("MESSAGE:Готов к интеграции с ML моделью")
    return 0

def process_documents(template_path, draft_path, output_dir):
    """
    Обрабатывает документы: применяет стили из шаблона к черновику
    TODO: Заменить на вызов ML модели
    """
    print("MESSAGE:Запуск DocStyler Python агента...")

    # 1. Читаем реальные файлы
    print("PROGRESS:20")
    print("MESSAGE:Чтение входных файлов...")

    try:
        with open(template_path, 'r', encoding='utf-8') as f:
            template_content = f.read()

        with open(draft_path, 'r', encoding='utf-8') as f:
            draft_content = f.read()
    except Exception as e:
        print(f"ERROR: Ошибка чтения файлов: {e}")
        return 1

    time.sleep(0.5)

    # 2. Анализ шаблона
    print("PROGRESS:40")
    print("MESSAGE:Анализ шаблона стилей...")

    # Извлекаем стили из шаблона (упрощённо)
    styles = []
    for line in template_content.split('\n'):
        line = line.strip()
        if line and '---' in line:
            # Пример: "Заголовок 1 --- Шрифт: Times New Roman, 14pt"
            part = line.split('---')[0].strip()
            styles.append(part)

    time.sleep(0.5)

    # 3. Обработка текста (ЗДЕСЬ БУДЕТ ML МОДЕЛЬ)
    print("PROGRESS:60")
    print("MESSAGE:Обработка документа...")

    # Временная логика: добавляем маркеры стилей к тексту
    processed_lines = []
    draft_lines = draft_content.strip().split('\n')

    for i, line in enumerate(draft_lines):
        if line.strip():
            # Простая эвристика для определения типа контента
            if i == 0 and len(line) < 50:
                # Первая короткая строка - вероятно заголовок
                processed_lines.append(f"# {line}")
                processed_lines.append(f"[Стиль: Заголовок 1 - {styles[0] if styles else 'Times New Roman, 14pt'}]")
            elif line in ["Введение", "Методы", "Результаты", "Заключение", "Список литературы"]:
                # Разделы
                processed_lines.append(f"## {line}")
                processed_lines.append(f"[Стиль: Заголовок 2 - {styles[1] if len(styles) > 1 else 'Arial, 12pt'}]")
            else:
                # Обычный текст
                processed_lines.append(line)
                processed_lines.append(f"[Стиль: Основной текст - {styles[2] if len(styles) > 2 else 'Calibri, 11pt'}]")
        processed_lines.append("")  # Пустая строка

    time.sleep(1)

    # 4. Сохранение результата
    print("PROGRESS:80")
    print("MESSAGE:Сохранение результата...")

    # Создаём директорию если нужно
    os.makedirs(output_dir, exist_ok=True)
    result_path = os.path.join(output_dir, "styled_document.txt")

    try:
        with open(result_path, 'w', encoding='utf-8') as f:
            # Заголовок документа
            f.write("=" * 70 + "\n")
            f.write("СТИЛИЗОВАННЫЙ ДОКУМЕНТ (DocStyler v1.0)\n")
            f.write("=" * 70 + "\n\n")

            # Метаданные
            f.write("[ИНФОРМАЦИЯ ОБ ОБРАБОТКЕ]\n")
            f.write(f"• Шаблон стилей: {os.path.basename(template_path)}\n")
            f.write(f"• Исходный документ: {os.path.basename(draft_path)}\n")
            f.write(f"• Время обработки: {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"• Агент: Python ML Bridge (готов к ML модели)\n\n")

            f.write("[ОБНАРУЖЕННЫЕ СТИЛИ В ШАБЛОНЕ]\n")
            for i, style in enumerate(styles[:3], 1):
                f.write(f"{i}. {style}\n")
            if not styles:
                f.write("Используются стили по умолчанию\n")
            f.write("\n")

            f.write("=" * 70 + "\n")
            f.write("ОБРАБОТАННОЕ СОДЕРЖАНИЕ:\n")
            f.write("=" * 70 + "\n\n")

            # Основной контент
            f.write("\n".join(processed_lines))
            f.write("\n\n")

            f.write("=" * 70 + "\n")
            f.write("ПРИМЕЧАНИЕ:\n")
            f.write("=" * 70 + "\n")
            f.write("Это временная версия. Скоро будет подключена ML модель для:\n")
            f.write("• Автоматического определения структуры документа\n")
            f.write("• Интеллектуального применения стилей\n")
            f.write("• Адаптивного форматирования\n")
            f.write("• Сохранения семантической разметки\n")

        print("PROGRESS:100")
        print("MESSAGE:Обработка успешно завершена!")
        print(f"RESULT:{result_path}")

        # Дополнительно: сохраняем сырой текст для ML модели
        raw_path = os.path.join(output_dir, "raw_for_ml.txt")
        with open(raw_path, 'w', encoding='utf-8') as f:
            f.write(draft_content)

        return 0

    except Exception as e:
        print(f"ERROR: Ошибка сохранения: {e}")
        return 1

def main():
    """Основная функция"""
    if len(sys.argv) > 1 and sys.argv[1] == "--test":
        return test_mode()

    if len(sys.argv) != 4:
        print("ERROR: Неправильные аргументы")
        print("Использование: python main.py <шаблон> <черновик> <выходная_директория>")
        print("Пример: python main.py template.txt draft.txt ./output")
        print("Тест: python main.py --test")
        return 2

    template_path = sys.argv[1]
    draft_path = sys.argv[2]
    output_dir = sys.argv[3]

    # Проверка файлов
    for path in [template_path, draft_path]:
        if not os.path.exists(path):
            print(f"ERROR: Файл не найден: {path}")
            return 1

    print(f"MESSAGE:Запуск обработки...")
    print(f"MESSAGE:Шаблон: {template_path}")
    print(f"MESSAGE:Черновик: {draft_path}")
    print(f"MESSAGE:Выходная директория: {output_dir}")

    return process_documents(template_path, draft_path, output_dir)

if __name__ == "__main__":
    try:
        exit_code = main()
        sys.exit(exit_code)
    except KeyboardInterrupt:
        print("\nMESSAGE:Прервано пользователем")
        sys.exit(130)
    except Exception as e:
        print(f"ERROR: Критическая ошибка: {e}")
        sys.exit(1)