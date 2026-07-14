#!/usr/bin/env python3
import re
from pathlib import Path
from collections import OrderedDict

def parse_properties_file_with_comments(filepath):
    """Parse a properties file preserving comments and empty lines."""
    entries = OrderedDict()  # key -> (value, preceding_lines)
    preceding_lines = []

    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n\r')

            # If it's a comment or empty line, collect it
            if not line.strip() or line.strip().startswith('#'):
                preceding_lines.append(line)
            # Parse key=value
            elif '=' in line:
                key, value = line.split('=', 1)
                key = key.strip()
                entries[key] = (value, preceding_lines)
                preceding_lines = []

    # Handle trailing comments/empty lines
    if preceding_lines:
        entries['__TRAILING__'] = ('', preceding_lines)

    return entries

def write_properties_file_with_comments(filepath, entries, reference_order):
    """Write properties preserving comments and empty lines."""
    with open(filepath, 'w', encoding='utf-8') as f:
        for key in reference_order:
            if key in entries:
                value, preceding_lines = entries[key]
                # Write preceding comments/empty lines
                for line in preceding_lines:
                    f.write(line + '\n')
                # Write the key=value
                f.write(f"{key}={value}\n")

        # Write any extra keys not in reference order
        for key in entries.keys():
            if key not in reference_order and key != '__TRAILING__':
                value, preceding_lines = entries[key]
                for line in preceding_lines:
                    f.write(line + '\n')
                f.write(f"{key}={value}\n")

        # Write trailing content
        if '__TRAILING__' in entries:
            value, preceding_lines = entries['__TRAILING__']
            for line in preceding_lines:
                f.write(line + '\n')

def main():
    base_dir = Path('/home/stokito/src/xmpp/Spark/core/src/main/resources/i18n')
    main_file = base_dir / 'spark_i18n.properties'

    # Parse the main reference file to get the key order
    main_entries = parse_properties_file_with_comments(main_file)
    reference_order = [k for k in main_entries.keys() if k != '__TRAILING__']

    print(f"Reference file has {len(reference_order)} keys")

    # Find all translation files
    translation_files = sorted([f for f in base_dir.glob('spark_i18n_*.properties')])

    for trans_file in translation_files:
        # Parse the translation file
        trans_entries = parse_properties_file_with_comments(trans_file)

        # Count statistics
        total_keys = len([k for k in trans_entries.keys() if k != '__TRAILING__'])
        missing_keys = len([k for k in reference_order if k not in trans_entries])
        extra_keys = len([k for k in trans_entries.keys() if k not in reference_order and k != '__TRAILING__'])

        # Rewrite with proper ordering
        write_properties_file_with_comments(trans_file, trans_entries, reference_order)

        lang = trans_file.name.replace('spark_i18n_', '').replace('.properties', '')
        print(f"✓ {trans_file.name}: {total_keys} keys ({missing_keys} missing, {extra_keys} extra)")

if __name__ == '__main__':
    main()
