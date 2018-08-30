SUMMARY = "Generates boot.mcs using vivado"
DESCRIPTION = "Manages task dependencies and creation of boot.mcs for microblaze"

LICENSE = "BSD"

PROVIDES = "virtual/boot-bin"

DEPENDS = "bitstream-microblaze"

PACKAGE_ARCH = "${MACHINE_ARCH}"

COMPATIBLE_MACHINE ?= "^$"
COMPATIBLE_MACHINE_microblaze = ".*"

inherit deploy

do_fetch[noexec] = "1"
do_unpack[noexec] = "1"
do_patch[noexec] = "1"

PROC ??= "kc705_i/microblaze_0"
PROC_kc705-microblazeel = "kc705_i/microblaze_0"

FLASH_SIZE ??= "0x80"
FLASH_INTERFACE ??= "BPIx16"

BITSTREAM_FILE ?= "${RECIPE_SYSROOT}/boot/bitstream/download.bit"
B = "${WORKDIR}/build"
WR_CFGMEM_MISC ?= "up 0 ${BITSTREAM_FILE}"

do_check_for_vivado() {
	bbnote "Checking Vivado install path"
	which "vivado" 2>/dev/null || {
		bbfatal "Vivado not found! Please add \"INHERIT += \"vivado\"\" to your local.conf"
  }
}

addtask do_check_for_vivado before do_configure

do_configure() {
    echo " write_cfgmem -force -format MCS -size ${FLASH_SIZE} -interface ${FLASH_INTERFACE} -loadbit \" ${WR_CFGMEM_MISC}\" ${B}/BOOT.mcs " > ${B}/write_cfgmem_boot_mcs.tcl
    if [ ! -e ${B}/write_cfgmem_boot_mcs.tcl ]; then
        bbfatal "write_cfgmem_boot_mcs.tcl creation failed. See log for details"
    fi
}


do_compile() {
    vivado -log "${B}/cfgmem_mcs.log" -jou "${B}/cfgmem_mcs.jou" -mode batch -s ${B}/write_cfgmem_boot_mcs.tcl
    if [ ! -e ${B}/BOOT.mcs ]; then
        bbfatal "BOOT.mcs failed. See log"
    fi
}

do_install() {
	:
}

BOOT_BASE_NAME ?= "boot-${MACHINE}-${DATETIME}"
BOOT_BASE_NAME[vardepsexclude] = "DATETIME"

DOWNLOADBIT_BASE_NAME ?= "download-${MACHINE}-${DATETIME}"
DOWNLOADBIT_BASE_NAME[vardepsexclude] = "DATETIME"

do_deploy() {
    #install BOOT.mcs
    if [ -e ${B}/BOOT.mcs ]; then
        install -Dm 0644 ${B}/BOOT.mcs ${DEPLOYDIR}/${BOOT_BASE_NAME}.mcs
        ln -sf ${BOOT_BASE_NAME}.mcs ${DEPLOYDIR}/BOOT-${MACHINE}.mcs
    fi
}
addtask do_deploy before do_build after do_compile