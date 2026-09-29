import re

with open("script0.txt", "r", errors="ignore") as f:
    content = f.read()

# Let's search for some patterns in WIZ_global_data
# In Google share links, the actual shared thread data is loaded inside an array.
# Let's find any text that looks like a user prompt or a Gemini response.
# We know the page had some text inside.
# Let's search for words like "icon", "VibeSync", "logo", "app" in the content.
words = ["icon", "vibesync", "logo", "design", "heart", "sound", "wave", "png", "jpg", "jpeg"]

for w in words:
    matches = list(re.finditer(re.escape(w), content, re.IGNORECASE))
    print(f"Word '{w}' has {len(matches)} matches.")
    for m in matches[:5]:
        start = max(0, m.start() - 100)
        end = min(len(content), m.end() + 100)
        print(f"  Context: {content[start:end].strip()}")
        print("-" * 30)
