#!/usr/bin/env python3
import re
import os
import glob

def convert_scss_to_css(content):
    """将 SCSS 嵌套语法转换为普通 CSS"""
    lines = content.split('\n')
    result = []
    parent_selectors = []
    indent_stack = []
    
    for line in lines:
        stripped = line.lstrip()
        indent = len(line) - len(stripped)
        
        # 跳过空行和注释
        if not stripped or stripped.startswith('//') or stripped.startswith('/*'):
            result.append(line)
            continue
        
        # 检查是否是嵌套选择器（以 & 开头）
        if stripped.startswith('&'):
            # 获取父选择器
            while indent_stack and indent <= indent_stack[-1]:
                parent_selectors.pop()
                indent_stack.pop()
            
            if parent_selectors:
                parent = parent_selectors[-1]
                # 替换 & 为父选择器
                new_selector = stripped.replace('&', parent, 1)
                result.append(' ' * indent + new_selector)
            else:
                result.append(line)
        else:
            # 普通选择器或属性
            if '{' in stripped:
                # 这是一个选择器
                selector = stripped.split('{')[0].strip()
                
                # 更新栈
                while indent_stack and indent <= indent_stack[-1]:
                    parent_selectors.pop()
                    indent_stack.pop()
                
                parent_selectors.append(selector)
                indent_stack.append(indent)
            
            result.append(line)
    
    return '\n'.join(result)

# 查找所有 .vue 文件
vue_files = glob.glob('/Users/dionysus/Downloads/power_manage 2/miniprogram/src/pages/**/*.vue', recursive=True)

for file_path in vue_files:
    print(f'Processing: {file_path}')
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 简单替换常见的 SCSS 嵌套模式
    # 替换 &. 为完整的类名
    content = re.sub(r'(\s+)&\.(\w+)', r'\1.\2', content)
    content = re.sub(r'(\s+)&\[', r'\1[', content)
    content = re.sub(r'(\s+)&:', r'\1:', content)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f'✓ Fixed: {file_path}')

print('All files processed!')

