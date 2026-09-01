---
trigger: always_on
glob:
description: Guidelines and workflows for interacting with Google NotebookLM MCP server to manage notebooks, add sources, and query research materials.
---

# NotebookLM Research Workspace Integration

This workspace is integrated with the **NotebookLM MCP Server** (`notebooklm-mcp` by PleasePrompto) using persistent browser sessions with Patchright.

## Registered Notebooks

- **Active Notebook**: `BÁO CÁO WEB` (`bao-cao-web`)
  - **URL**: `https://notebooklm.google.com/notebook/a6c0a3aa-4416-4b3f-98be-d26e5437db02?authuser=1`
  - **ID**: `a6c0a3aa-4416-4b3f-98be-d26e5437db02`
  - **Topics**: Báo cáo, Lập trình web, Java Project, Tài liệu môn học

## Capabilities & Available Operations

When interacting with NotebookLM via MCP tools:
1. **Notebook Management**:
   - `list_notebooks`: Lists all registered notebooks.
   - `select_notebook`: Selects the active default notebook.
   - `add_notebook`: Registers a new notebook.
2. **Source Ingestion**:
   - `add_source`: Ingests URLs, Drive files, or raw text into the notebook.
3. **Query & Grounded Research**:
   - `ask_question`: Asks queries strictly grounded in the notebook sources.
4. **Audio Overview**:
   - `generate_audio` / `download_audio`: Generates audio podcast overviews.
