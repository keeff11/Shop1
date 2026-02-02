"use client";

import React, { useCallback, useEffect, useState, useRef } from "react";
import { cn } from "@/lib/utils";

interface DualRangeSliderProps {
  min: number;
  max: number;
  step?: number;
  value: [number, number];
  onValueChange: (value: [number, number]) => void;
  className?: string;
  formatLabel?: (value: number) => string;
}

const DualRangeSlider = ({
  min,
  max,
  step = 1,
  value,
  onValueChange,
  className,
  formatLabel,
}: DualRangeSliderProps) => {
  const [localValue, setLocalValue] = useState(value);
  const sliderRef = useRef<HTMLDivElement>(null);
  const isDragging = useRef<"min" | "max" | null>(null);
  
  // [핵심 수정 1] 최신 값을 저장하는 Ref (튕김 방지용)
  // 이벤트 리스너가 '옛날 값'을 참조하지 않도록 Ref에 최신 상태를 계속 업데이트합니다.
  const valueRef = useRef(localValue);
  valueRef.current = localValue;

  // 부모의 value prop이 바뀌면 로컬 상태 동기화 (드래그 중이 아닐 때만)
  useEffect(() => {
    if (!isDragging.current) {
        setLocalValue(value);
    }
  }, [value]);

  const getPercentage = useCallback(
    (val: number) => ((val - min) / (max - min)) * 100,
    [min, max]
  );

  // 마우스 이동 핸들러
  const handleMouseMove = useCallback(
    (e: MouseEvent) => {
      if (!isDragging.current || !sliderRef.current) return;

      const rect = sliderRef.current.getBoundingClientRect();
      const percent = Math.min(Math.max((e.clientX - rect.left) / rect.width, 0), 1);
      const rawValue = min + percent * (max - min);
      const newValue = Math.round(rawValue / step) * step;

      setLocalValue((prev) => {
        const next = [...prev] as [number, number];
        if (isDragging.current === "min") {
          next[0] = Math.min(newValue, prev[1] - step);
        } else {
          next[1] = Math.max(newValue, prev[0] + step);
        }
        return next;
      });
    },
    [min, max, step]
  );

  // 마우스 떼기 핸들러
  const handleMouseUp = useCallback(() => {
    isDragging.current = null;
    document.removeEventListener("mousemove", handleMouseMove);
    document.removeEventListener("mouseup", handleMouseUp);
    
    // [핵심 수정 2] Ref에 저장된 '최신 값'을 사용하여 최종 반영
    onValueChange(valueRef.current);
  }, [handleMouseMove, onValueChange]);

  // 드래그 중 실시간 업데이트
  useEffect(() => {
    if (isDragging.current) {
        onValueChange(localValue);
    }
  }, [localValue, onValueChange]);

  const handleMouseDown = (type: "min" | "max") => (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    isDragging.current = type;
    document.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseup", handleMouseUp);
  };

  // 눈금(Ticks) 렌더링 (9개로 증가)
  const renderTicks = () => {
    const tickCount = 9; // 0%, 12.5%, 25%, ... 100%
    return Array.from({ length: tickCount }).map((_, i) => {
      const percent = (i / (tickCount - 1)) * 100;
      // 짝수 번째(0, 25, 50...)는 조금 길게, 홀수 번째는 짧게 디자인
      const isMajor = i % 2 === 0; 
      return (
        <div 
            key={i} 
            className="absolute top-2.5 flex flex-col items-center -translate-x-1/2 pointer-events-none"
            style={{ left: `${percent}%` }}
        >
            <div className={cn(
                "rounded-full bg-gray-300", 
                isMajor ? "h-2 w-0.5" : "h-1 w-0.5"
            )}></div>
        </div>
      );
    });
  };

  return (
    <div className={cn("relative w-full h-8 flex items-center select-none touch-none mb-2", className)} ref={sliderRef}>
      {/* 눈금 렌더링 */}
      {renderTicks()}

      {/* 배경 트랙 */}
      <div className="absolute w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
        <div
          className="absolute h-full bg-black/90"
          style={{
            left: `${getPercentage(localValue[0])}%`,
            width: `${getPercentage(localValue[1]) - getPercentage(localValue[0])}%`,
          }}
        />
      </div>

      {/* Min Thumb (핸들) */}
      <div
        className="absolute w-5 h-5 bg-white border-[2.5px] border-black rounded-full shadow-md cursor-grab active:cursor-grabbing hover:scale-110 transition-transform z-10 flex items-center justify-center focus:ring-2 focus:ring-black/20 focus:outline-none"
        style={{ left: `calc(${getPercentage(localValue[0])}% - 10px)` }}
        onMouseDown={handleMouseDown("min")}
        role="slider"
        aria-valuenow={localValue[0]}
      />

      {/* Max Thumb (핸들) */}
      <div
        className="absolute w-5 h-5 bg-white border-[2.5px] border-black rounded-full shadow-md cursor-grab active:cursor-grabbing hover:scale-110 transition-transform z-10 flex items-center justify-center focus:ring-2 focus:ring-black/20 focus:outline-none"
        style={{ left: `calc(${getPercentage(localValue[1])}% - 10px)` }}
        onMouseDown={handleMouseDown("max")}
        role="slider"
        aria-valuenow={localValue[1]}
      />
    </div>
  );
};

export default DualRangeSlider;