import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React from 'react';
import {
  Dimensions,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View
} from 'react-native';

const { height } = Dimensions.get('window');

export default function HomeScreen() {
  const router = useRouter();

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView 
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* 히어로 섹션 (상단 메인) */}
        <View style={styles.heroSection}>
          {/* 장식용 배경 효과 (모바일에서는 심플한 원형 도형으로 대체) */}
          <View style={[styles.bgCircle, styles.circleBlue]} />
          <View style={[styles.bgCircle, styles.circlePurple]} />

          <View style={styles.badge}>
            <Text style={styles.badgeText}>New Concept Shopping Mall</Text>
          </View>

          <Text style={styles.mainTitle}>
            모든 가치를 한 곳에,{"\n"}
            <Text style={styles.highlightText}>Shop1</Text>에서 시작하세요
          </Text>

          <Text style={styles.subtitle}>
            엄선된 제품과 차별화된 경험을 제공합니다.{"\n"}
            지금 바로 Shop1의 다양한 카테고리를 탐험해보세요.
          </Text>

          <View style={styles.buttonContainer}>
            <TouchableOpacity 
              style={styles.primaryButton}
              onPress={() => router.push('/items')} // 상품 리스트 페이지로 이동
              activeOpacity={0.8}
            >
              <Text style={styles.primaryButtonText}>전체 상품 보러가기</Text>
            </TouchableOpacity>
            
            <TouchableOpacity 
              style={styles.secondaryButton}
              activeOpacity={0.8}
            >
              <Text style={styles.secondaryButtonText}>이벤트 확인하기</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* 특징(Features) 섹션 - 모바일에 맞게 세로 배치 */}
        <View style={styles.featuresSection}>
          <View style={styles.featureItem}>
            <View style={styles.iconContainer}>
              <Ionicons name="rocket" size={28} color="#3b82f6" />
            </View>
            <Text style={styles.featureTitle}>빠른 배송</Text>
            <Text style={styles.featureDesc}>주문 후 24시간 이내에{"\n"}발송을 원칙으로 합니다.</Text>
          </View>

          <View style={styles.featureItem}>
            <View style={styles.iconContainer}>
              <Ionicons name="diamond" size={28} color="#8b5cf6" />
            </View>
            <Text style={styles.featureTitle}>엄선된 품질</Text>
            <Text style={styles.featureDesc}>모든 제품은 전문가의{"\n"}검수를 거쳐 입고됩니다.</Text>
          </View>

          <View style={styles.featureItem}>
            <View style={styles.iconContainer}>
              <Ionicons name="lock-closed" size={28} color="#10b981" />
            </View>
            <Text style={styles.featureTitle}>안전한 결제</Text>
            <Text style={styles.featureDesc}>당신의 소중한 정보는{"\n"}강력하게 보호됩니다.</Text>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  scrollContent: {
    flexGrow: 1,
  },
  heroSection: {
    minHeight: height * 0.65,
    paddingHorizontal: 24,
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
    overflow: 'hidden',
  },
  bgCircle: {
    position: 'absolute',
    borderRadius: 200,
    opacity: 0.15,
  },
  circleBlue: {
    width: 300,
    height: 300,
    backgroundColor: '#3b82f6',
    top: -50,
    left: -100,
  },
  circlePurple: {
    width: 250,
    height: 250,
    backgroundColor: '#8b5cf6',
    bottom: -50,
    right: -80,
  },
  badge: {
    backgroundColor: 'rgba(59, 130, 246, 0.1)',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    marginBottom: 24,
  },
  badgeText: {
    color: '#3b82f6',
    fontWeight: '700',
    fontSize: 13,
    letterSpacing: 0.5,
  },
  mainTitle: {
    fontSize: 34,
    fontWeight: '900',
    color: '#111827',
    textAlign: 'center',
    lineHeight: 44,
    marginBottom: 20,
  },
  highlightText: {
    color: '#4f46e5',
  },
  subtitle: {
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 40,
  },
  buttonContainer: {
    width: '100%',
    gap: 12,
  },
  primaryButton: {
    backgroundColor: '#111827',
    paddingVertical: 18,
    borderRadius: 14,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  primaryButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  secondaryButton: {
    backgroundColor: '#ffffff',
    paddingVertical: 18,
    borderRadius: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  secondaryButtonText: {
    color: '#374151',
    fontSize: 16,
    fontWeight: 'bold',
  },
  featuresSection: {
    backgroundColor: '#f9fafb',
    paddingHorizontal: 24,
    paddingVertical: 48,
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
  },
  featureItem: {
    alignItems: 'center',
    marginBottom: 40,
  },
  iconContainer: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
  },
  featureTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#111827',
    marginBottom: 8,
  },
  featureDesc: {
    fontSize: 14,
    color: '#6b7280',
    textAlign: 'center',
    lineHeight: 22,
  },
});