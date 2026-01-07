set -euo pipefail

source=${1:-"tmp/_source"}

get_files() {
  find app -type f -print
  find db -type f -print
  find srv -type f -name "*.cds" -print
}

old_files=$(get_files)
echo "Removing old files..."
echo "$old_files"
for f in $old_files; do
  rm -f "$f"
done

# Copy from source, preserving folders
target=$(pwd)
pushd "$source"
new_files=$(get_files)
echo "Copying new files..."
echo "$new_files"
for f in $new_files; do
  mkdir -p "$target/$(dirname "$f")"
  cp "$f" "$target/$f"
done
popd
