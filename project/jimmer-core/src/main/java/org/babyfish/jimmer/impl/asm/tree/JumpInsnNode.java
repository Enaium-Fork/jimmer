// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.babyfish.jimmer.impl.asm.tree;

import java.util.Map;
import org.babyfish.jimmer.impl.asm.MethodVisitor;
import org.babyfish.jimmer.impl.asm.tree.LabelNode;

/**
 * A node that represents a jump instruction. A jump instruction is an instruction that may jump to
 * another instruction.
 *
 * @author Eric Bruneton
 */
public class JumpInsnNode extends AbstractInsnNode {

  /**
   * The operand of this instruction. This operand is a label that designates the instruction to
   * which this instruction may jump.
   */
  public LabelNode label;

  /**
   * Constructs a new {@link JumpInsnNode}.
   *
   * @param opcode the opcode of the type instruction to be constructed. This opcode must be IFEQ,
   *     IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT,
   *     IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
   * @param label the operand of the instruction to be constructed. This operand is a label that
   *     designates the instruction to which the jump instruction may jump.
   */
  public JumpInsnNode(final int opcode, final LabelNode label) {
    super(opcode);
    this.label = label;
  }

  /**
   * Sets the opcode of this instruction.
   *
   * @param opcode the new instruction opcode. This opcode must be IFEQ, IFNE, IFLT, IFGE, IFGT,
   *     IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
   *     IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
   */
  public void setOpcode(final int opcode) {
    this.opcode = opcode;
  }

  @Override
  public int getType() {
    return JUMP_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitJumpInsn(opcode, label.getLabel());
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new JumpInsnNode(opcode, clone(label, clonedLabels)).cloneAnnotations(this);
  }
}
