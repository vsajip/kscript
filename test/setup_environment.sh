#!/usr/bin/env bash

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIR=$(realpath "$SCRIPT_DIR/../")
KSCRIPT_EXEC_DIR="$PROJECT_DIR/build/libs"

echo "Setting up environment..."
echo "SCRIPT_DIR :        $SCRIPT_DIR"
echo "PROJECT_DIR:        $PROJECT_DIR"
echo "KSCRIPT_EXEC_DIR:   $KSCRIPT_EXEC_DIR"
echo

export PATH=$KSCRIPT_EXEC_DIR:$PATH
echo  "KScript path for testing: $(which kscript)"

if [[ ! -f "$KSCRIPT_EXEC_DIR/assert.sh" ]]; then
  echo "Installing assert.sh"
  wget --quiet -O "$KSCRIPT_EXEC_DIR/assert.sh" https://raw.githubusercontent.com/lehmannro/assert.sh/master/assert.sh
  chmod u+x "$KSCRIPT_EXEC_DIR/assert.sh"
fi

export DEBUG="--verbose"
. assert.sh

# Fake idea binary just printing passed arguments...
echo "#!/usr/bin/env bash" > "${KSCRIPT_EXEC_DIR}/idea"
echo "echo \$*" >> "${KSCRIPT_EXEC_DIR}/idea"

## define test helper, see https://github.com/lehmannro/assert.sh/issues/24
assert_statement(){
    # usage cmd exp_stout exp_stder exp_exit_code
    assert "$1" "$2"
    assert "( $1 ) 2>&1 >/dev/null" "$3"
    assert_raises "$1" "$4"
}
#assert_statment "echo foo; echo bar  >&2; exit 1" "foo" "bar" 1


assert_stderr(){
    assert "( $1 ) 2>&1 >/dev/null" "$2"
}

#http://stackoverflow.com/questions/3005963/how-can-i-have-a-newline-in-a-string-in-sh
export NL=$'\n'

kscript --clear-cache
