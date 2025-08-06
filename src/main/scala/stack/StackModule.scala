package stack

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import java.nio.file.Paths

// Your code starts here

class StackModule(val dataWidth: Int, val len: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(32.W))
    val out = Output(UInt(dataWidth.W))
    val underflow = Output(Bool())
    val overflow = Output(Bool())
    val isEmpty = Output(Bool())
    val isFull = Output(Bool())
    val popped = Output(Bool())
    val peeked = Output(Bool())
  })

  // Stack memory - implemented as a vector of registers
  val stack = RegInit(VecInit(Seq.fill(len)(0.U(dataWidth.W))))
  val stackPointer = RegInit(0.U(log2Ceil(len + 1).W))
  
  // Extract instruction components
  val opcode = io.in(6, 0)
  val immediate = io.in(31, 7)
  
  // Instruction opcodes
  val PUSH_OP = "b0100111".U
  val POP_OP = "b1000011".U 
  val PEEK_OP = "b1000000".U
  
  // Default output values
  io.out := 0.U
  io.underflow := false.B
  io.overflow := false.B
  io.popped := false.B
  io.peeked := false.B
  
  // Stack status signals
  io.isEmpty := stackPointer === 0.U
  io.isFull := stackPointer === len.U
  
  // Reset behavior
  when (reset.asBool) {
    stackPointer := 0.U
    for (i <- 0 until len) {
      stack(i) := 0.U
    }
  } .otherwise {
    // Instruction decoding and execution
    switch(opcode) {
      is(PUSH_OP) {
        // Push instruction
        when(stackPointer < len.U) {
          // Stack not full - push the value
          val pushValue = immediate(dataWidth - 1, 0)
          stack(stackPointer) := pushValue
          stackPointer := stackPointer + 1.U
          io.overflow := false.B
        } .otherwise {
          // Stack is full - assert overflow
          io.overflow := true.B
        }
      }
      
      is(POP_OP) {
        // Pop instruction  
        when(stackPointer > 0.U) {
          // Stack not empty - pop the value
          stackPointer := stackPointer - 1.U
          io.out := stack(stackPointer - 1.U)
          io.popped := true.B
          io.underflow := false.B
        } .otherwise {
          // Stack is empty - assert underflow
          io.out := 0.U
          io.underflow := true.B
          io.popped := false.B
        }
      }
      
      is(PEEK_OP) {
        // Peek instruction
        when(stackPointer > 0.U) {
          // Stack not empty - peek at top value
          io.out := stack(stackPointer - 1.U)
          io.peeked := true.B
          io.underflow := false.B
        } .otherwise {
          // Stack is empty - assert underflow  
          io.out := 0.U
          io.underflow := true.B
          io.peeked := false.B
        }
      }
    }
  }
}

// Your code ends here

object SVGen extends App {
  val out = Paths.get(
    "out",
    this.getClass
      .getName
      .stripSuffix("$")
  ).toString
  new ChiselStage().emitSystemVerilog(
    new StackModule(args(0).toInt, args(1).toInt),
    Array("--target-dir", out),
  )
}
