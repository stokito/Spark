#!/usr/bin/env python3
"""
Translate untranslated strings using available translation APIs.
"""

import json
import time
import sys
from pathlib import Path
from typing import Dict, List, Tuple
import urllib.request
import urllib.parse

# Base directory
BASE_DIR = Path("./core/src/main/resources/i18n")
ENGLISH_FILE = BASE_DIR / "spark_i18n.properties"

def parse_properties(file_path: Path) -> Dict[str, str]:
    """Parse a Java properties file."""
    props = {}
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#'):
                    if '=' in line:
                        key, value = line.split('=', 1)
                        props[key] = value
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
    return props

def translate_mymemory(text: str, target_lang: str) -> str:
    """Translate using MyMemory API with retry."""
    if not text or len(text) > 1500:
        return None
    
    for attempt in range(3):
        try:
            encoded = urllib.parse.quote(text)
            url = f"https://api.mymemory.translated.net/get?q={encoded}&langpair=en|{target_lang}"
            
            with urllib.request.urlopen(url, timeout=10) as response:
                data = json.loads(response.read().decode('utf-8'))
                if data.get('responseStatus') == 200:
                    translated = data.get('responseData', {}).get('translatedText', '')
                    if translated and translated != text:
                        return translated
        except Exception as e:
            print(f"Error translated {text}: {e}")
            if attempt < 2:
                time.sleep(5)
            continue
    
    return None

def translate_batch(strings: List[str], target_lang: str) -> Dict[str, str]:
    """Translate a batch of strings."""
    results = {}
    for i, text in enumerate(strings):
        translated = translate_mymemory(text, target_lang)
        if translated:
            results[text] = translated
        
        # Rate limiting
        if (i + 1) % 5 == 0:
            time.sleep(0.2)
        
        if (i + 1) % 50 == 0:
            print(f"  Progress: {i + 1}/{len(strings)}")
    
    return results

def update_language_file(lang_code: str) -> Tuple[int, int]:
    """Update translation file for a language."""
    file_path = BASE_DIR / f"spark_i18n_{lang_code}.properties"
    target_lang = lang_code
    
    # Load English source
    english_props = parse_properties(ENGLISH_FILE)
    
    # Load existing translations
    existing_props = parse_properties(file_path)
    
    # Find missing keys
    missing_keys = [k for k in english_props.keys() if k not in existing_props]
    
    if not missing_keys:
        print(f"✓ {lang_code}: All strings translated")
        return 0, 0
    
    print(f"Translating {lang_code}: {len(missing_keys)} missing strings...")
    
    # Translate
    translated_count = 0
    failed_count = 0
    
    for key in missing_keys:
        print(f"{key}")

        english_text = english_props[key]
#         translated_text = translate_mymemory(english_text, target_lang)
        translated_text = english_text

        if translated_text:
            existing_props[key] = translated_text
            translated_count += 1
        else:
            failed_count += 1
        
        # Rate limiting
        if (translated_count + failed_count) % 5 == 0:
            time.sleep(0.1)
    
    # Write back file - preserve original structure
    with open(file_path, 'r', encoding='utf-8') as f:
        original_lines = f.readlines()
    
    # Rebuild file with comments preserved
    new_lines = []
    processed_keys = set()
    
    # First pass: preserve comments and structure
    for line in original_lines:
        stripped = line.strip()
        if stripped.startswith('#') or not stripped:
            new_lines.append(line)
        elif '=' in stripped:
            key = stripped.split('=', 1)[0]
            if key in english_props and key in existing_props:
                new_lines.append(f"{key}={existing_props[key]}\n")
                processed_keys.add(key)
    
    # Second pass: add any new translations not in original file
    for key in english_props.keys():
        if key not in processed_keys and key in existing_props:
            new_lines.append(f"{key}={existing_props[key]}\n")
    
    # Write file
    with open(file_path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
    
    print(f"  → {translated_count} translated, {failed_count} failed")
    return translated_count, failed_count

def main():
    target_langs = ['cs', 'de', 'es', 'fi', 'fr', 'it', 'ja', 'ko', 'ky', 'lt', 'nl', 'pl', 'pt_BR', 'pt_PT', 'ru', 'sv', 'tr', 'uk', 'zh_CN', 'zh_TW',
    'am', 'ar', 'bn', 'yo', 'bg','sl','el','fa','hr', 'ku',  'sr', 'sw', 'hi','jv','ro','vi', 'tl', 'th', 'my']

    print("Starting translation process...")
    print(f"Target languages: {', '.join(target_langs)}")
    print()
    
    total_translated = 0
    total_failed = 0
    
    for lang_code in target_langs:
        translated, failed = update_language_file(lang_code)
        total_translated += translated
        total_failed += failed
        print()
    
    print(f"Translation complete!")
    print(f"Total: {total_translated} translated, {total_failed} failed")

if __name__ == '__main__':
    main()
